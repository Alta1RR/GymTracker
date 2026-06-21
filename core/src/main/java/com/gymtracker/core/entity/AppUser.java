package com.gymtracker.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Table(name = "app_users")
@Entity
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long telegramId;

    private String telegramUserName;

    @CreationTimestamp
    private LocalDateTime dateOfRegistration;

    private Integer weight; // В килограммах

    private Integer height; // В сантиметрах

    @Enumerated(EnumType.STRING)
    private Gender gender;

    public enum Gender{
        MALE,
        FEMALE
    }
}
