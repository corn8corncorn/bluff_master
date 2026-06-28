package com.bluffmaster.dto;

import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;
import com.bluffmaster.model.GameRound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private FakeImageRegion fakeImageRegion;
    private FakeImageCategory fakeImageCategory;
    private Boolean fakeThemeSelected;  // 是否已選假圖主題
    private String fakeThemeLabel;      // 主題顯示名稱（台灣 · 美食）
    private String speakerFakeImageUrl;  // 主講者選擇的說謊圖片
    private GameRound.RoundPhase phase;  // 回合階段
    private Map<String, String> votes;
    private Boolean isFinished;
    private Integer votingTimeLeft;  // 剩餘投票時間（秒）
    private LocalDateTime votingEndsAt;  // 投票截止時間
    private Map<String, VoteResult> voteResults;  // 每個玩家的投票結果和得分變化
}

