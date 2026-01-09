package com.questevent.service;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserWalletService userWalletService;

    public UserService(UserRepository userRepository, UserWalletService userWalletService) {
        this.userRepository = userRepository;
        this.userWalletService = userWalletService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));    }

    public User addUser(User user) {

        userRepository.save(user);
        userWalletService.createWalletForUser(user);
        return user;

    }

    public User updateUser(Long id, User updatedUser) {

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

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<SimpleGrantedAuthority> getAuthorities(User user) {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    public UserResponseDto convertToDto(User user) {
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
