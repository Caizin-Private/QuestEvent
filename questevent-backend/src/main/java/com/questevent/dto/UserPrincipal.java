package com.questevent.dto;

import com.questevent.enums.Role;

public record UserPrincipal(
        Long userId,
        String email,
        Role role
) {

}

