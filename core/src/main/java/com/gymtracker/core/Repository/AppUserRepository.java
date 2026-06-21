package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    public Optional<AppUser> findByTelegramId(Long telegramId);
}
