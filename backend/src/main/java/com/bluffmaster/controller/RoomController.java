package com.bluffmaster.controller;

import com.bluffmaster.dto.*;
import com.bluffmaster.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDTO room = roomService.createRoom(request);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomDTO> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        RoomDTO room = roomService.joinRoom(request);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable String roomId) {
        RoomDTO room = roomService.getRoom(roomId);
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
}

