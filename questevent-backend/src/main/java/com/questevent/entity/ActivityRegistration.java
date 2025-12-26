package com.questevent.entity;
 
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
 
 
@Entity
@Table(name = "activity_registrations")
@Data
public class ActivityRegistration {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
 
 
    @ManyToMany
    @JoinColumn(
            name = "activity_id",
            foreignKey = @ForeignKey(name = "activity_id")
    )
    private Activity activity_id;
 
    @OneToMany
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "user_id")
    )
    private User user_id;
 
 
    @Column(name = "earned_gems")
    private Integer earned_gems;
 
    @Column(name = "submission_url")
    private String submission_url;
 
}
 