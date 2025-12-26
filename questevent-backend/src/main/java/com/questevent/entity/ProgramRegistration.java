package com.questevent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.apache.catalina.User;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
  name = "program_registrations",
  uniqueConstraints = @UniqueConstraint(columnNames = {"program_id","user_id"})
)
public class ProgramRegistration {

    @Id @GeneratedValue
    private Long programRegistrationId;

    @ManyToOne
    private Program program;

    private User user;

    private LocalDateTime registeredAt = LocalDateTime.now();
}
