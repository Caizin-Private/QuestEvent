package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.Role;
import lombok.Data;

@Data
public class UserResponseDto {
    private Long userId;
    private String name;
    private String email;
    private Department department;
    private String gender;
    private Role role;
}
