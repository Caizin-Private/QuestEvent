package com.questevent.service;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserWalletService userWalletService;

    public UserService(UserRepository userRepository, UserWalletService userWalletService) {
        this.userRepository = userRepository;
        this.userWalletService = userWalletService;
    }

    public List<User> getAllUsers() {

        log.debug("Fetching all users");

        List<User> users = userRepository.findAll();

        log.info("Fetched all users | count={}", users.size());

        return users;
    }

    public User getUserById(Long id) {

        log.debug("Fetching user by id | userId={}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found | userId={}", id);
                    return new UserNotFoundException("User not found with id " + id);
                });
    }

    public User addUser(User user) {

        log.debug(
                "Adding new user | email={} | role={}",
                user.getEmail(),
                user.getRole()
        );

        userRepository.save(user);
        userWalletService.createWalletForUser(user);

        log.info(
                "User created successfully | userId={}",
                user.getUserId()
        );

        return user;
    }

    public User updateUser(Long id, User updatedUser) {

        log.debug("Updating user | userId={}", id);

        User user = getUserById(id);

        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setDepartment(updatedUser.getDepartment());
        user.setGender(updatedUser.getGender());
        user.setRole(updatedUser.getRole());

        if(updatedUser.getWallet() != null) {
            updatedUser.getWallet().setUser(user);
            user.setWallet(updatedUser.getWallet());
        }

        if(updatedUser.getHostedPrograms() != null)
            user.setHostedPrograms(updatedUser.getHostedPrograms());

        if(updatedUser.getActivityRegistrations() != null)
            user.setActivityRegistrations(updatedUser.getActivityRegistrations());

        if(updatedUser.getProgramRegistrations() != null)
            user.setProgramRegistrations(updatedUser.getProgramRegistrations());

        if(updatedUser.getProgramWallets() != null)
            user.setProgramWallets(updatedUser.getProgramWallets());

        User saved = userRepository.save(user);

        log.info(
                "User updated successfully | userId={}",
                saved.getUserId()
        );

        return saved;
    }

    public void deleteUser(Long id) {

        log.debug("Deleting user | userId={}", id);

        userRepository.deleteById(id);

        log.info("User deleted | userId={}", id);
    }

    public List<SimpleGrantedAuthority> getAuthorities(User user) {

        log.debug(
                "Resolving authorities | userId={} | role={}",
                user.getUserId(),
                user.getRole()
        );

        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    public UserResponseDto convertToDto(User user) {

        log.debug(
                "Converting User to UserResponseDto | userId={}",
                user.getUserId()
        );

        UserResponseDto userDto = new UserResponseDto();
        userDto.setUserId(user.getUserId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setDepartment(user.getDepartment());
        userDto.setGender(user.getGender());
        userDto.setRole(user.getRole());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());

        return userDto;
    }
}
