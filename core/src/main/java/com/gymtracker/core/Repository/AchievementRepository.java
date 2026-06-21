package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.Achievement;
import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.Friendship;
import com.gymtracker.core.entity.UserAchievement;
import com.gymtracker.core.entity.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
}
