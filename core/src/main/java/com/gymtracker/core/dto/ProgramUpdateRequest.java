package com.gymtracker.core.dto;

import lombok.Data;

@Data
public class ProgramUpdateRequest {
    private Long telegramId;
    private String name;
}
