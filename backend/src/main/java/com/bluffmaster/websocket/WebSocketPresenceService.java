package com.bluffmaster.websocket;

import com.bluffmaster.dto.RoomDTO;
import com.bluffmaster.model.Player;
import com.bluffmaster.repository.PlayerRepository;
import com.bluffmaster.service.GameService;
import com.bluffmaster.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketPresenceService {

    private final WebSocketSessionRegistry sessionRegistry;
    private final GameService gameService;
    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPresenceService(
            WebSocketSessionRegistry sessionRegistry,
            GameService gameService,
            RoomService roomService,
            PlayerRepository playerRepository,
            @Lazy SimpMessagingTemplate messagingTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.gameService = gameService;
        this.roomService = roomService;
        this.playerRepository = playerRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void onConnect(String playerId, String roomId, String sessionId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        boolean becameOnline = sessionRegistry.register(playerId, sessionId);
        if (!becameOnline) {
            log.debug("WebSocket 重連（仍有其他連線）: playerId={}", playerId);
            return;
        }
        log.info("玩家上線: playerId={}", playerId);
        gameService.handlePlayerReconnect(playerId);
        broadcastRoom(resolveRoomId(playerId, roomId));
    }

    public void onDisconnect(String playerId, String roomId, String sessionId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        boolean becameOffline = sessionRegistry.unregister(playerId, sessionId);
        if (!becameOffline) {
            log.debug("WebSocket 斷線（仍有其他連線）: playerId={}", playerId);
            return;
        }
        log.info("玩家離線: playerId={}", playerId);
        gameService.handlePlayerDisconnect(playerId);
        String resolvedRoomId = resolveRoomId(playerId, roomId);
        if (resolvedRoomId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + resolvedRoomId + "/player-disconnect", playerId);
            broadcastRoom(resolvedRoomId);
        }
    }

    private String resolveRoomId(String playerId, String roomId) {
        if (roomId != null && !roomId.isBlank()) {
            return roomId;
        }
        return playerRepository.findById(playerId)
                .map(Player::getRoomId)
                .orElse(null);
    }

    private void broadcastRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        try {
            RoomDTO room = roomService.getRoom(roomId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
        } catch (Exception e) {
            log.warn("廣播房間狀態失敗: roomId={}", roomId, e);
        }
    }
}
