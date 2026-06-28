package com.bluffmaster.websocket;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.dto.RoomDTO;
import com.bluffmaster.service.GameService;
import com.bluffmaster.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final GameService gameService;

    @MessageMapping("/room/update")
    public void broadcastRoomUpdate(@Payload String roomId) {
        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
    }

    @MessageMapping("/game/start-round")
    public void startRound(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        String playerId = payload.get("playerId");
        GameRoundDTO round = gameService.startRound(roomId, playerId);

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);

        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
    }

    @MessageMapping("/game/vote")
    public void vote(@Payload Map<String, Object> payload) {
        String roundId = (String) payload.get("roundId");
        String playerId = (String) payload.get("playerId");
        String imageUrl = (String) payload.get("imageUrl");

        gameService.vote(playerId, imageUrl, roundId);

        String roomId = (String) payload.get("roomId");
        Map<String, Object> voteStatus = Map.of(
            "playerId", playerId,
            "imageUrl", imageUrl,
            "roundId", roundId
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/vote", voteStatus);
    }

    @MessageMapping("/game/next-phase")
    public void nextPhase(@Payload Map<String, String> payload) {
        log.info("收到 next-phase 請求: {}", payload);
        String roundId = payload.get("roundId");
        String playerId = payload.get("playerId");
        GameRoundDTO round = gameService.nextPhase(roundId, playerId);
        String roomId = round.getRoomId();
        log.info("廣播回合更新到房間 {}: phase={}, roundId={}", roomId, round.getPhase(), round.getId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);
    }

    @MessageMapping("/game/start-voting")
    public void startVoting(@Payload Map<String, String> payload) {
        log.info("收到 start-voting 請求: {}", payload);
        String roundId = payload.get("roundId");
        String playerId = payload.get("playerId");
        GameRoundDTO round = gameService.startVoting(roundId, playerId);
        String roomId = round.getRoomId();
        log.info("廣播回合更新到房間 {}: phase={}, roundId={}", roomId, round.getPhase(), round.getId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);
    }

    @MessageMapping("/game/reveal")
    public void revealResult(@Payload Map<String, String> payload) {
        String roundId = payload.get("roundId");
        String playerId = payload.get("playerId");
        GameRoundDTO round = gameService.revealResult(roundId, playerId);
        String roomId = round.getRoomId();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);
    }

    @MessageMapping("/game/select-fake-theme")
    public void selectFakeTheme(@Payload Map<String, String> payload) {
        try {
            String roundId = payload.get("roundId");
            String playerId = payload.get("playerId");
            String region = payload.get("region");
            String category = payload.get("category");
            GameRoundDTO round = gameService.selectFakeTheme(roundId, region, category, playerId);
            String roomId = round.getRoomId();
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round", round);
        } catch (Exception e) {
            log.error("select-fake-theme 失敗: {}", payload, e);
        }
    }

    @MessageMapping("/game/finish-round")
    public void finishRound(@Payload Map<String, String> payload) {
        String roundId = payload.get("roundId");
        String playerId = payload.get("playerId");
        GameRoundDTO round = gameService.finishRound(roundId, playerId);
        String roomId = round.getRoomId();

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/round-result", round);

        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
    }
}
