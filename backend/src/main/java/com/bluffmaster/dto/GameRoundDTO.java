package com.bluffmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRoundDTO {
    private String id;
    private String roomId;
    private Integer roundNumber;
    private String speakerId;
    private String speakerNickname;
    private List<String> imageUrls;
    private String fakeImageUrl;
    private Map<String, String> votes;
    private Boolean isFinished;
    private Integer votingTimeLeft;  // 剩餘投票時間（秒）
}

