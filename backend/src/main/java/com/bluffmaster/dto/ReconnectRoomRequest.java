package com.bluffmaster.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconnectRoomRequest {
    @NotBlank(message = "房間代碼不能為空")
    private String roomCode;

    @NotBlank(message = "暱稱不能為空")
    private String nickname;

    /** 可選：瀏覽器 localStorage 保存的原 playerId，優先用來認領座位 */
    private String playerId;
}
