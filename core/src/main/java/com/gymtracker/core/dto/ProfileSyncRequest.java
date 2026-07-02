package com.gymtracker.core.dto;

import lombok.Data;

@Data
public class ProfileSyncRequest {
    private Long telegramId;
    private String telegramUserName;
    private String photoUrl;
}
