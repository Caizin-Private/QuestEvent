package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    public User addUser(User user) {
        if(user.getWallet() != null) {
            user.getWallet().setUser(user);
            userWalletService.createWalletForUser(user);
        }
        return userRepository.save(user);
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
}
