package com.gymtracker.core.dto;

import lombok.Data;

@Data
public class WorkoutSetDto {
    private Long exerciseId;
    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Boolean extraSet;
}
