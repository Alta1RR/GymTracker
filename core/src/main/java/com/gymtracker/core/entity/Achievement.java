package com.gymtracker.core.entity;

import com.gymtracker.core.entity.enums.AchievementTrigger;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String icon; // Потом сгенерить медальки и поменять на картинки

    @Enumerated(EnumType.STRING)
    private AchievementTrigger triggerType;

    private Integer triggerValue;
}
