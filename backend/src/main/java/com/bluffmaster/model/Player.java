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
@Table(name = "players")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private Boolean isHost;

    @Column(nullable = false)
    private Boolean isReady;

    @Column(nullable = false)
    private Integer score;

    @ElementCollection
    @CollectionTable(name = "player_images", joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls;

    @Column(nullable = false)
    private Boolean isOnline;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        if (score == null) {
            score = 0;
        }
        if (isReady == null) {
            isReady = false;
        }
        if (isOnline == null) {
            isOnline = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

