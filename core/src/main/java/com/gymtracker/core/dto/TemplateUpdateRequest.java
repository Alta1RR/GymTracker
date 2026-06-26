package com.gymtracker.core.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateUpdateRequest {
    private Long telegramId;
    private String name;
    private List<TemplateExerciseUpdateRequest> exercises;
}
