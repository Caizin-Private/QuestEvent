package com.questevent.dto;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long userId;
    private String name;
    private String email;
    private String department;
    private String gender;
    private String role;
}
