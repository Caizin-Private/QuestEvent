package com.questevent.dto;

import com.questevent.enums.Role;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        String email,
        Role role
){

}
