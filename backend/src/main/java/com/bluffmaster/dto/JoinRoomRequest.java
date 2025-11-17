package com.bluffmaster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    @NotBlank(message = "房間代碼不能為空")
    private String roomCode;

    @NotBlank(message = "暱稱不能為空")
    private String nickname;
}

