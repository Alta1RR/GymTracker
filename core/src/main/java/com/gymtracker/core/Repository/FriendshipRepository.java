package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.Friendship;
import com.gymtracker.core.entity.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // Найти запрос между двумя конкретными юзерами
    Optional<Friendship> findByAppUserAndRecipient(AppUser appUser, AppUser recipient);

    // Найти все одобренные запросы, где я был отправителем
    List<Friendship> findByAppUserAndStatus(AppUser appUser, FriendshipStatus status);

    // Найти все одобренные запросы, где я был получателем
    List<Friendship> findByRecipientAndStatus(AppUser recipient, FriendshipStatus status);

    void deleteByAppUserOrRecipient(AppUser appUser, AppUser recipient);
}
