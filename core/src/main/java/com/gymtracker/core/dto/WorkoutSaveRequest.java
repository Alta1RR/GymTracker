package com.gymtracker.core.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkoutSaveRequest {
    private Long telegramId;
    private String workoutName;
    private List<WorkoutSetDto> sets;
    private Integer durationInSeconds;
    private Long programId;
    private Integer qualityScorePercent;
    private Double plannedVolume;
    private Double actualVolume;
}
