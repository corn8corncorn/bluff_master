package com.bluffmaster.config;

import com.bluffmaster.websocket.WebSocketPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    private final WebSocketPresenceService presenceService;

    public StompChannelInterceptor(@Lazy WebSocketPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String playerId = accessor.getFirstNativeHeader("playerId");
            String roomId = accessor.getFirstNativeHeader("roomId");

            if (playerId != null && !playerId.isBlank()) {
                accessor.getSessionAttributes().put("playerId", playerId);
                if (roomId != null && !roomId.isBlank()) {
                    accessor.getSessionAttributes().put("roomId", roomId);
                }
                log.info("WebSocket CONNECT: playerId={}, roomId={}", playerId, roomId);
                try {
                    presenceService.onConnect(playerId, roomId, accessor.getSessionId());
                } catch (Exception e) {
                    log.warn("WebSocket 連線狀態更新失敗: playerId={}", playerId, e);
                }
            }
        }

        return message;
    }
}
