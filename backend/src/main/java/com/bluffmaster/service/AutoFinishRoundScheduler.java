package com.bluffmaster.service;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.dto.RoomDTO;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * reveal 後延遲自動 finishRound，避免房主離線卡在 REVEALING。
 * 若房主提前手動 finish，會取消對應排程。
 */
@Component
@Slf4j
public class AutoFinishRoundScheduler {

  static final long AUTO_FINISH_DELAY_SECONDS = 4;

  private final GameService gameService;
  private final RoomService roomService;
  private final SimpMessagingTemplate messagingTemplate;
  private final ConcurrentHashMap<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "auto-finish-round");
            t.setDaemon(true);
            return t;
          });

  public AutoFinishRoundScheduler(
      @Lazy GameService gameService,
      RoomService roomService,
      SimpMessagingTemplate messagingTemplate) {
    this.gameService = gameService;
    this.roomService = roomService;
    this.messagingTemplate = messagingTemplate;
  }

  public void scheduleAfterReveal(String roundId, String roomId) {
    if (roundId == null || roomId == null) {
      return;
    }
    cancel(roundId);
    ScheduledFuture<?> future =
        executor.schedule(
            () -> {
              pending.remove(roundId);
              try {
                GameRoundDTO round = gameService.finishRound(roundId, null);
                messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/round-result", round);
                RoomDTO room = roomService.getRoom(roomId);
                messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
                log.info("自動完成回合計分: roomId={}, roundId={}", roomId, roundId);
              } catch (Exception e) {
                log.warn("自動完成回合失敗: roundId={}", roundId, e);
              }
            },
            AUTO_FINISH_DELAY_SECONDS,
            TimeUnit.SECONDS);
    pending.put(roundId, future);
    log.info(
        "已排程 {} 秒後自動 finishRound: roomId={}, roundId={}",
        AUTO_FINISH_DELAY_SECONDS,
        roomId,
        roundId);
  }

  public void cancel(String roundId) {
    if (roundId == null) {
      return;
    }
    ScheduledFuture<?> future = pending.remove(roundId);
    if (future != null && future.cancel(false)) {
      log.info("已取消自動 finishRound（可能已手動完成）: roundId={}", roundId);
    }
  }
}
