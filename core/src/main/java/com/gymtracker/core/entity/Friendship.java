package com.gymtracker.core.entity;

import com.gymtracker.core.entity.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester", nullable = false)
    private AppUser appUser;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private  AppUser recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;
}
