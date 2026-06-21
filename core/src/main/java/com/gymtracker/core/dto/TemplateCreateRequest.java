package com.gymtracker.core.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateCreateRequest {
    private String name;
    private List<Long> exerciseIds;
}
