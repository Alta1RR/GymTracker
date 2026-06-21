package com.gymtracker.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
// Получение медали. Связывание юзера с полученной ачивкой
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AppUser user;

    @ManyToOne
    private Achievement achievement;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
