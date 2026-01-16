package com.questevent.service;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User addUser(User user) {
        return userRepository.save(user);
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

    public UserResponseDto getCurrentUser(Jwt jwt) {

        String email = jwt.getClaimAsString("email");

        log.info("Fetching current user email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found for email: " + email
                        )
                );

        return convertToDto(user);
    }

    public User updateCurrentUser(Jwt jwt, User updatedData) {

        String email = jwt.getClaimAsString("email");

        log.info("Updating user profile email={}", email);

        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found for email: " + email
                        )
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
}
