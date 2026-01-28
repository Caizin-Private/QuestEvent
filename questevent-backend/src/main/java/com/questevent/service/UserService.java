package com.questevent.service;

import com.questevent.dto.CompleteProfileRequest;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserWalletService userWalletService;
    private final SecurityUserResolver securityUserResolver;

    public User addUser(User user) {
        log.debug( "Adding new user | email={} | role={}",
                user.getEmail(),
                user.getRole() );

        User savedUser = userRepository.save(user);

        log.info( "User created successfully | userId={}",
                user.getUserId() );

        return savedUser;
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id: " + userId)
                );

        return convertToDto(user);
    }

    public UserResponseDto getCurrentUser() {

        User user = securityUserResolver.getCurrentUser();

        log.info(
                "Fetching current user | userId={} | email={}",
                user.getUserId(),
                user.getEmail()
        );

        return convertToDto(user);
    }
    public User updateCurrentUser(User updatedData) {

        User existingUser = securityUserResolver.getCurrentUser();

        log.info(
                "Updating user profile | userId={} | email={}",
                existingUser.getUserId(),
                existingUser.getEmail()
        );


        // ✅ SAFE FIELDS ONLY
        existingUser.setName(updatedData.getName());
        existingUser.setDepartment(updatedData.getDepartment());
        existingUser.setGender(updatedData.getGender());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(
                    "User not found with id: " + userId
            );
        }

        userRepository.deleteById(userId);
    }

    public UserResponseDto convertToDto(User user) {

        return UserResponseDto.from(user);

    }

    public User completeProfile(
            @Valid CompleteProfileRequest request) {

        User user = securityUserResolver.getCurrentUser();

        // already completed → no-op
        if (user.getDepartment() != null && user.getGender() != null) {
            return user;
        }

        user.setDepartment(request.department());
        user.setGender(request.gender());

        return userRepository.save(user);
    }
}
