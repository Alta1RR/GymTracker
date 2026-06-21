package com.gymtracker.core.dto;

import com.gymtracker.core.entity.enums.ProgressionType;
import lombok.Data;

@Data
public class ProgramCreateRequest {
    private Long telegramId;
    private String name;
    private ProgressionType progressionType;
}
