package com.bluffmaster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 6)
    private String roomCode;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Column(nullable = false)
    private GameMode gameMode;

    @Column(nullable = false)
    private RoomStatus status;

    @ElementCollection
    @CollectionTable(name = "room_players", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "player_id")
    private List<String> playerIds;

    @Column(nullable = false)
    private Integer currentRound;

    @Column(nullable = false)
    private Integer totalRounds;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime startedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (playerIds == null) {
            playerIds = new ArrayList<>();
        }
        if (currentRound == null) {
            currentRound = 0;
        }
        if (status == null) {
            status = RoomStatus.WAITING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GameMode {
        NORMAL,  // 一般模式
        QUICK    // 快速模式
    }

    public enum RoomStatus {
        WAITING,    // 等待中
        PLAYING,    // 遊戲中
        FINISHED    // 已結束
    }
}

