package com.gymtracker.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

// Сущность упражнения внутри шаблона тренировки
@Data
@Entity
@Table(name = "template_exercise")
public class TemplateExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    private Integer sequenceOrder;

    private Integer targetSets = 3;

    @JsonIgnore
    @ManyToOne
    private WorkoutTemplate workoutTemplate;

    @ManyToOne
    private Exercise exercise;
}
