package com.bluffmaster.websocket;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 追蹤每位玩家的 WebSocket 連線數，避免換頁重連時舊連線斷開誤標為離線。
 */
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, Set<String>> sessionsByPlayer = new ConcurrentHashMap<>();

    /** @return true 若此為該玩家第一條連線（由離線變在線） */
    public boolean register(String playerId, String sessionId) {
        if (playerId == null || playerId.isBlank() || sessionId == null) {
            return false;
        }
        Set<String> sessions = sessionsByPlayer.computeIfAbsent(
                playerId, id -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = sessions.isEmpty();
        sessions.add(sessionId);
        return wasEmpty;
    }

    /** @return true 若此為該玩家最後一條連線（由在線變離線） */
    public boolean unregister(String playerId, String sessionId) {
        if (playerId == null || playerId.isBlank() || sessionId == null) {
            return false;
        }
        Set<String> sessions = sessionsByPlayer.get(playerId);
        if (sessions == null) {
            return false;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByPlayer.remove(playerId);
            return true;
        }
        return false;
    }
}
