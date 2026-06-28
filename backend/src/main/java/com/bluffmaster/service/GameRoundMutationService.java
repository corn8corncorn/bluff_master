package com.bluffmaster.service;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;
import com.bluffmaster.model.GameRound;
import com.bluffmaster.model.Player;
import com.bluffmaster.repository.GameRoundRepository;
import com.bluffmaster.repository.PlayerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoundMutationService {

  private final GameRoundRepository gameRoundRepository;
  private final PlayerRepository playerRepository;
  private final GameRoundDtoMapper gameRoundDtoMapper;

  @Transactional
  public GameRoundDTO applyFakeThemeAndBuildDto(
      String roundId,
      FakeImageRegion region,
      FakeImageCategory category,
      String fakeUrl,
      String actorPlayerId) {
    GameRound round =
        gameRoundRepository
            .findById(roundId)
            .orElseThrow(() -> new RuntimeException("回合不存在"));

    requireSpeaker(round, actorPlayerId);

    if (Boolean.TRUE.equals(round.getIsFinished())) {
      throw new RuntimeException("回合已結束");
    }
    if (round.getPhase() != GameRound.RoundPhase.STORY_TELLING) {
      throw new RuntimeException("只能在講故事階段選擇假圖主題");
    }
    if (round.getFakeImageUrl() != null && !round.getFakeImageUrl().isBlank()) {
      throw new RuntimeException("假圖主題已選擇");
    }

    List<String> images = round.getImageUrls();
    if (images == null || images.isEmpty()) {
      images = new ArrayList<>();
    } else {
      images = new ArrayList<>(images);
    }
    images.add(fakeUrl);
    Collections.shuffle(images);
    round.setImageUrls(images);

    round.setFakeImageUrl(fakeUrl);
    round.setFakeImageRegion(region);
    round.setFakeImageCategory(category);

    GameRound saved = gameRoundRepository.saveAndFlush(round);

    if (saved.getImageUrls() != null) {
      saved.getImageUrls().size();
    }
    if (saved.getVotes() != null) {
      saved.getVotes().size();
    }

    Player speaker =
        playerRepository
            .findById(saved.getSpeakerId())
            .orElseThrow(() -> new RuntimeException("主講者不存在"));

    log.info("假圖已寫入回合 {}: {}", roundId, fakeUrl);
    return gameRoundDtoMapper.toDto(saved, speaker.getNickname());
  }

  private void requireSpeaker(GameRound round, String playerId) {
    if (playerId == null || playerId.isBlank()) {
      throw new RuntimeException("缺少玩家身份");
    }
    Player player =
        playerRepository
            .findById(playerId)
            .orElseThrow(() -> new RuntimeException("玩家不存在"));
    if (!player.getRoomId().equals(round.getRoomId())) {
      throw new RuntimeException("玩家不在該房間");
    }
    if (round.getSpeakerId() == null || !round.getSpeakerId().equals(playerId)) {
      throw new RuntimeException("只有主講者可以執行此操作");
    }
  }
}
