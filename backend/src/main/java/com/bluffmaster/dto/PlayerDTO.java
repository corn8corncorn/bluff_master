package com.bluffmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDTO {
    private String id;
    private String nickname;
    private String roomId;
    private Boolean isHost;
    private Boolean isReady;
    private Integer score;
    private List<String> imageUrls;
    private Boolean isOnline;
}

