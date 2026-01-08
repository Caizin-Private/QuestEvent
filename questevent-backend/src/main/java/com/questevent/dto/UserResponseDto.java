package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.Role;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResponseDto {
    private UUID userId;
    private String name;
    private String email;
    private Department department;
    private String gender;
    private Role role;
}
