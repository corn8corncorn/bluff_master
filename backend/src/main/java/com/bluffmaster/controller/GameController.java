package com.bluffmaster.controller;

import com.bluffmaster.dto.FakeThemeOptionsDTO;
import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.dto.RoomDTO;
import com.bluffmaster.dto.VoteRequest;
import com.bluffmaster.repository.PlayerRepository;
import com.bluffmaster.service.GameService;
import com.bluffmaster.service.RoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private static final String SESSION_PLAYER_ID = "playerId";

    private final GameService gameService;
    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/rooms/{roomId}/rounds/start")
    public ResponseEntity<GameRoundDTO> startRound(
            @PathVariable String roomId,
            HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        GameRoundDTO round = gameService.startRound(roomId, playerId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);
        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
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

    @PostMapping("/rounds/{roundId}/next-phase")
    public ResponseEntity<GameRoundDTO> nextPhase(
            @PathVariable String roundId,
            HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        GameRoundDTO round = gameService.nextPhase(roundId, playerId);
        return ResponseEntity.ok(round);
    }

    @PostMapping("/rounds/{roundId}/start-voting")
    public ResponseEntity<GameRoundDTO> startVoting(
            @PathVariable String roundId,
            HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        GameRoundDTO round = gameService.startVoting(roundId, playerId);
        return ResponseEntity.ok(round);
    }

    @PostMapping("/rounds/{roundId}/reveal")
    public ResponseEntity<GameRoundDTO> revealResult(
            @PathVariable String roundId,
            HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        GameRoundDTO round = gameService.revealResult(roundId, playerId);
        return ResponseEntity.ok(round);
    }

    @PostMapping("/rounds/{roundId}/finish")
    public ResponseEntity<GameRoundDTO> finishRound(
            @PathVariable String roundId,
            HttpSession session) {
        String playerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        GameRoundDTO round = gameService.finishRound(roundId, playerId);
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
        playerRepository.findById(playerId).ifPresent(player -> {
            RoomDTO room = roomService.getRoom(player.getRoomId());
            messagingTemplate.convertAndSend("/topic/room/" + player.getRoomId(), room);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fake-theme-options")
    public ResponseEntity<FakeThemeOptionsDTO> getFakeThemeOptions() {
        return ResponseEntity.ok(gameService.getFakeThemeOptions());
    }

    @PostMapping("/rounds/{roundId}/select-fake-theme")
    public ResponseEntity<GameRoundDTO> selectFakeTheme(
            @PathVariable String roundId,
            @RequestParam String region,
            @RequestParam String category,
            @RequestParam(required = false) String playerId,
            HttpSession session) {
        String sessionPlayerId = (String) session.getAttribute(SESSION_PLAYER_ID);
        String actorId = sessionPlayerId != null ? sessionPlayerId : playerId;
        if (actorId == null || actorId.isBlank()) {
            throw new RuntimeException("缺少玩家身份，請重新加入房間");
        }
        GameRoundDTO round = gameService.selectFakeTheme(roundId, region, category, actorId);
        messagingTemplate.convertAndSend("/topic/room/" + round.getRoomId() + "/round", round);
        return ResponseEntity.ok(round);
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
