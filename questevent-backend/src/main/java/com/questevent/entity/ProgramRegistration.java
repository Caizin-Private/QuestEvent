package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(
  name = "program_registrations",
  uniqueConstraints = @UniqueConstraint(columnNames = {"program_id","user_id"})
)
public class ProgramRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_registration_id")
    private Long programRegistrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "program_id")
    )
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "user_id")
    )
    @JsonIgnore
    private User user;

    @Column(name = "registered_at", updatable = false)
    private Instant registeredAt = Instant.now();

}
