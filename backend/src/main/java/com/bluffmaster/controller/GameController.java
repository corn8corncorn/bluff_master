package com.bluffmaster.controller;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.dto.VoteRequest;
import com.bluffmaster.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/rooms/{roomId}/rounds/start")
    public ResponseEntity<GameRoundDTO> startRound(@PathVariable String roomId) {
        GameRoundDTO round = gameService.startRound(roomId);
        return ResponseEntity.ok(round);
    }

    @PostMapping("/rounds/{roundId}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable String roundId,
            @RequestParam String playerId,
            @Valid @RequestBody VoteRequest request) {
        gameService.vote(playerId, request.getImageUrl(), roundId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rounds/{roundId}/finish")
    public ResponseEntity<GameRoundDTO> finishRound(@PathVariable String roundId) {
        GameRoundDTO round = gameService.finishRound(roundId);
        return ResponseEntity.ok(round);
    }

    @PostMapping("/players/{playerId}/disconnect")
    public ResponseEntity<Void> handleDisconnect(@PathVariable String playerId) {
        gameService.handlePlayerDisconnect(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/players/{playerId}/reconnect")
    public ResponseEntity<Void> handleReconnect(@PathVariable String playerId) {
        gameService.handlePlayerReconnect(playerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms/{roomId}/current-round")
    public ResponseEntity<GameRoundDTO> getCurrentRound(@PathVariable String roomId) {
        GameRoundDTO round = gameService.getCurrentRound(roomId);
        if (round == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(round);
    }
}

