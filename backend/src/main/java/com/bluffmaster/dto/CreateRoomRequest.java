package com.bluffmaster.dto;

import com.bluffmaster.model.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    @NotBlank(message = "暱稱不能為空")
    private String nickname;

    @NotNull(message = "遊戲模式不能為空")
    private Room.GameMode gameMode;

    @Min(value = 2, message = "最少需要 2 人")
    @Max(value = 10, message = "最多 10 人")
    @NotNull(message = "人數不能為空")
    private Integer maxPlayers;
}

