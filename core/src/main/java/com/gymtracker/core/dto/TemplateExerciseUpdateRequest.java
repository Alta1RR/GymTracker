package com.gymtracker.core.dto;

import lombok.Data;

@Data
public class TemplateExerciseUpdateRequest {
    private Long exerciseId;
    private Integer targetSets;
}
