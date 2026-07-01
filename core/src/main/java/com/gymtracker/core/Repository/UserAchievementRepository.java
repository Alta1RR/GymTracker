package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.Achievement;
import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUser(AppUser user);

    void deleteByUser(AppUser user);

    boolean existsByUserAndAchievement(AppUser user, Achievement achievement);
}
