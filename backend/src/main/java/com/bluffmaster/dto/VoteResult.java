package com.bluffmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    private String playerId;
    private String playerNickname;
    private String votedImageUrl;  // 玩家投票的圖片 URL，null 表示未投票
    private boolean isCorrect;    // 是否投中假圖
    private int scoreChange;      // 得分變化（+1, -1, 0）
    private boolean isSpeaker;    // 是否為主講者
}

