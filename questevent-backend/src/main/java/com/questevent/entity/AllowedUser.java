package com.questevent.entity;

import jakarta.persistence.*;
@Entity
@Table(name = "allowed_users")
public class AllowedUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
}
