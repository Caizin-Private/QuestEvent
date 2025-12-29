package com.questevent.entity;
 
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;
 
 
@Entity
@Table(name = "activity_registrations",
uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id","user_id"}))
@Data
public class ActivityRegistration {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long activityRegistrationId;

    @ManyToOne
    @JoinColumn(
            name = "activity_id",
            foreignKey = @ForeignKey(name = "activity_id")
    )
    private Activity activity;

    @ManyToOne
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "user_id")
    )
    private User user;

    @Column(name = "earned_gems")
    private Integer earnedGems;

    @Column(name = "submission_url")
    private String submissionUrl;
 
}
 