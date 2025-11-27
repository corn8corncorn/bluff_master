package com.bluffmaster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "game_rounds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private String speakerId;  // 主講者 ID

    @ElementCollection
    @CollectionTable(name = "round_images", joinColumns = @JoinColumn(name = "round_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls;  // 包含 3 真圖 + 1 假圖

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fakeImageUrl;  // 假圖 URL

    @Column(columnDefinition = "TEXT")
    private String speakerFakeImageUrl;  // 主講者選擇的說謊圖片 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundPhase phase;  // 回合階段

    @ElementCollection
    @CollectionTable(name = "round_votes", joinColumns = @JoinColumn(name = "round_id"))
    @MapKeyColumn(name = "player_id")
    @Column(name = "voted_image_url", columnDefinition = "TEXT")
    private Map<String, String> votes;  // playerId -> imageUrl

    @Column(nullable = false)
    private Boolean isFinished;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime finishedAt;

    public enum RoundPhase {
        STORY_TELLING,  // 講故事階段
        QUESTIONING,    // 發問階段
        VOTING,         // 投票階段
        REVEALING,      // 公布結果階段
        FINISHED        // 已完成
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (votes == null) {
            votes = new HashMap<>();
        }
        if (isFinished == null) {
            isFinished = false;
        }
        if (phase == null) {
            phase = RoundPhase.STORY_TELLING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (isFinished && finishedAt == null) {
            finishedAt = LocalDateTime.now();
        }
    }
}

