package com.bluffmaster.controller;

import com.bluffmaster.dto.*;
import com.bluffmaster.service.RoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final String SESSION_PLAYER_ID = "playerId";
    private static final String SESSION_ROOM_ID = "roomId";

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody CreateRoomRequest request, HttpSession session) {
        RoomDTO room = roomService.createRoom(request);
        
        // 將玩家 ID 和房間 ID 保存到 session
        if (room.getPlayers() != null && !room.getPlayers().isEmpty()) {
            // 找到房主（創建房間的玩家）
            String hostPlayerId = room.getPlayers().stream()
                    .filter(PlayerDTO::getIsHost)
                    .findFirst()
                    .map(PlayerDTO::getId)
                    .orElse(null);
            
            if (hostPlayerId != null) {
                session.setAttribute(SESSION_PLAYER_ID, hostPlayerId);
                session.setAttribute(SESSION_ROOM_ID, room.getId());
                log.info("Session 已保存: playerId={}, roomId={}", hostPlayerId, room.getId());
            }
        }
        
        return ResponseEntity.ok(room);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomDTO> joinRoom(@Valid @RequestBody JoinRoomRequest request, HttpSession session) {
        RoomDTO room = roomService.joinRoom(request);
        
        // 檢查 session 中是否已經有玩家 ID 和房間 ID
        String existingPlayerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        String existingRoomId = (String) session.getAttribute(SESSION_ROOM_ID);
        
        // 如果 session 中已經有玩家且房間 ID 匹配，檢查該玩家是否仍在房間中
        if (existingPlayerId != null && room.getId().equals(existingRoomId)) {
            boolean playerExists = room.getPlayers().stream()
                    .anyMatch(p -> p.getId().equals(existingPlayerId));
            
            if (playerExists) {
                // Session 中的玩家仍在房間中，不覆蓋 session（可能是多標籤頁情況）
                log.info("Session 中已有玩家 {} 在房間 {} 中，不覆蓋 session", existingPlayerId, room.getId());
            } else {
                // Session 中的玩家不在房間中，更新為新加入的玩家
                String newPlayerId = room.getPlayers().stream()
                        .filter(p -> p.getNickname().equals(request.getNickname()))
                        .findFirst()
                        .map(PlayerDTO::getId)
                        .orElse(null);
                
                if (newPlayerId != null) {
                    session.setAttribute(SESSION_PLAYER_ID, newPlayerId);
                    session.setAttribute(SESSION_ROOM_ID, room.getId());
                    log.info("Session 已更新: playerId={}, roomId={}", newPlayerId, room.getId());
                }
            }
        } else {
            // Session 中沒有玩家或房間 ID 不匹配，保存新玩家的 session
            if (room.getPlayers() != null && !room.getPlayers().isEmpty()) {
                // 找到剛加入的玩家（通過暱稱匹配）
                String newPlayerId = room.getPlayers().stream()
                        .filter(p -> p.getNickname().equals(request.getNickname()))
                        .findFirst()
                        .map(PlayerDTO::getId)
                        .orElse(null);
                
                if (newPlayerId != null) {
                    session.setAttribute(SESSION_PLAYER_ID, newPlayerId);
                    session.setAttribute(SESSION_ROOM_ID, room.getId());
                    log.info("Session 已保存: playerId={}, roomId={}", newPlayerId, room.getId());
                }
            }
        }
        
        // 廣播房間更新，通知其他玩家有新玩家加入
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), room);
        log.info("廣播房間更新: 房間 {} 有新玩家加入", room.getId());
        
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable String roomId, HttpSession session) {
        RoomDTO room = roomService.getRoom(roomId);
        
        // 從 session 中獲取玩家 ID（如果存在）
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        String sessionRoomId = (String) session.getAttribute(SESSION_ROOM_ID);
        
        log.debug("getRoom: roomId={}, sessionRoomId={}, sessionPlayerId={}", 
                roomId, sessionRoomId, playerId);
        
        // 驗證 session 中的房間 ID 是否匹配
        if (playerId != null && roomId.equals(sessionRoomId)) {
            // 驗證玩家是否仍在房間中
            boolean playerExists = room.getPlayers().stream()
                    .anyMatch(p -> p.getId().equals(playerId));
            
            if (!playerExists) {
                // 玩家不在房間中，清除 session
                session.removeAttribute(SESSION_PLAYER_ID);
                session.removeAttribute(SESSION_ROOM_ID);
                log.info("玩家 {} 不在房間 {} 中，已清除 session", playerId, roomId);
            } else {
                log.debug("getRoom: Session 驗證通過，玩家 {} 在房間 {} 中", playerId, roomId);
            }
        } else {
            // Session 中的房間 ID 不匹配或為空
            if (playerId != null && !roomId.equals(sessionRoomId)) {
                log.warn("getRoom: Session 中的房間 ID ({}) 與請求的房間 ID ({}) 不匹配，但不清除 session（可能是多標籤頁）", 
                        sessionRoomId, roomId);
            }
        }
        
        return ResponseEntity.ok(room);
    }

    @GetMapping("/code/{roomCode}")
    public ResponseEntity<RoomDTO> getRoomByCode(@PathVariable String roomCode) {
        RoomDTO room = roomService.getRoomByCode(roomCode);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/players/{playerId}/ready")
    public ResponseEntity<Void> playerReady(
            @PathVariable String roomId,
            @PathVariable String playerId) {
        roomService.playerReady(playerId);
        
        // 廣播房間更新
        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/players/{playerId}/cancel-ready")
    public ResponseEntity<Void> playerCancelReady(
            @PathVariable String roomId,
            @PathVariable String playerId) {
        roomService.playerCancelReady(playerId);
        
        // 廣播房間更新
        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
        
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomId}/players/{playerId}/nickname")
    public ResponseEntity<Void> updateNickname(
            @PathVariable String roomId,
            @PathVariable String playerId,
            @RequestBody String newNickname) {
        roomService.updatePlayerNickname(playerId, newNickname);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/host/{hostId}/kick/{targetPlayerId}")
    public ResponseEntity<Void> kickPlayer(
            @PathVariable String roomId,
            @PathVariable String hostId,
            @PathVariable String targetPlayerId) {
        roomService.kickPlayer(hostId, targetPlayerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/host/{hostId}/start")
    public ResponseEntity<Void> startGame(
            @PathVariable String roomId,
            @PathVariable String hostId) {
        roomService.startGame(hostId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<RoomDTO> leaveRoom(@PathVariable String roomId, HttpSession session) {
        // 從 session 獲取玩家 ID
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        String sessionRoomId = (String) session.getAttribute(SESSION_ROOM_ID);
        
        if (playerId == null || !roomId.equals(sessionRoomId)) {
            throw new RuntimeException("無法識別玩家身份");
        }
        
        // 執行退出邏輯（包括房主轉移）
        RoomDTO updatedRoom = roomService.leaveRoom(playerId);
        
        // 清除 session
        session.removeAttribute(SESSION_PLAYER_ID);
        session.removeAttribute(SESSION_ROOM_ID);
        log.info("玩家 {} 已離開房間 {}，session 已清除", playerId, roomId);
        
        // 廣播房間更新，通知其他玩家
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updatedRoom);
        log.info("廣播房間更新: 房間 {} 有玩家退出", roomId);
        
        return ResponseEntity.ok(updatedRoom);
    }

    @GetMapping("/session/player-id")
    public ResponseEntity<String> getPlayerIdFromSession(HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        return ResponseEntity.ok(playerId);
    }
}

