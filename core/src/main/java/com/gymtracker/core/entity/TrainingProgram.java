package com.gymtracker.core.entity;

import com.gymtracker.core.entity.enums.ProgressionType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

// Класс сущности программы тренировок
@Data
@Table(name = "training_program")
@Entity
public class TrainingProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressionType progressionType;

    // Счетчик недель
    private Integer currentWeek;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @OneToMany(mappedBy = "trainingProgram", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<WorkoutTemplate> templates;
}