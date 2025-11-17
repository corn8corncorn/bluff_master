package com.bluffmaster.websocket;

import com.bluffmaster.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("收到新的 WebSocket 連接");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        
        if (playerId != null) {
            log.info("玩家斷線: " + playerId);
            gameService.handlePlayerDisconnect(playerId);
            
            // 通知房間其他玩家
            String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
            if (roomId != null) {
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/player-disconnect", playerId);
            }
        }
    }
}

