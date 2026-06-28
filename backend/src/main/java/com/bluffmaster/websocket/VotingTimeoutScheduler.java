package com.bluffmaster.websocket;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VotingTimeoutScheduler {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 1000)
    public void autoRevealExpiredVotingRounds() {
        List<GameRoundDTO> revealed = gameService.revealExpiredVotingRounds();
        for (GameRoundDTO round : revealed) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + round.getRoomId() + "/round", round);
            log.info("投票逾時自動公布結果: roomId={}, roundId={}", round.getRoomId(), round.getId());
        }
    }
}
