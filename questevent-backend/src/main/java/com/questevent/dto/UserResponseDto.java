package com.questevent.dto;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;

public record UserResponseDto(
        Long userId,
        String name,
        String email,
        Department department,
        String gender,
        Role role,
        boolean profileCompleted
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getGender(),
                user.getRole(),
                isProfileCompleted(user)
        );
    }

    private static boolean isProfileCompleted(User user) {
        return user.getDepartment() != null &&
               user.getGender() != null &&
               !user.getGender().isBlank();
    }
}
