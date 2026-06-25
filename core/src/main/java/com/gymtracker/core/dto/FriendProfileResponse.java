package com.gymtracker.core.dto;

import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.UserAchievement;
import com.gymtracker.core.entity.Workout;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FriendProfileResponse {
    private AppUser friend;
    private Long totalWorkouts;
    private Long totalTrainingMinutes;
    private List<Workout> history;
    private List<UserAchievement> achievements;
}