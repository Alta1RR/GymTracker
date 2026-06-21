package com.gymtracker.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

// Сущность шаблона тренировки
@Data
@Entity
@Table(name = "workout_template")
public class WorkoutTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "program_id", nullable = true)
    private TrainingProgram trainingProgram;

    @OneToMany(mappedBy = "workoutTemplate", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TemplateExercise> templates;
}
