package com.bluffmaster.service;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.model.GameRound;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameRoundDtoMapper {

  private final FakeImageService fakeImageService;

  public GameRoundDTO toDto(GameRound round, String speakerNickname) {
    boolean themeSelected =
        round.getFakeImageUrl() != null && !round.getFakeImageUrl().isBlank();
    String themeLabel = null;
    if (round.getFakeImageRegion() != null && round.getFakeImageCategory() != null) {
      themeLabel =
          fakeImageService.getThemeLabel(
              round.getFakeImageRegion(), round.getFakeImageCategory());
    }

    GameRoundDTO.GameRoundDTOBuilder builder =
        GameRoundDTO.builder()
            .id(round.getId())
            .roomId(round.getRoomId())
            .roundNumber(round.getRoundNumber())
            .speakerId(round.getSpeakerId())
            .speakerNickname(speakerNickname)
            .imageUrls(
                round.getImageUrls() != null
                    ? new java.util.ArrayList<>(round.getImageUrls())
                    : null)
            .fakeImageUrl(round.getFakeImageUrl())
            .fakeImageRegion(round.getFakeImageRegion())
            .fakeImageCategory(round.getFakeImageCategory())
            .fakeThemeSelected(themeSelected)
            .fakeThemeLabel(themeLabel)
            .speakerFakeImageUrl(round.getSpeakerFakeImageUrl())
            .phase(round.getPhase())
            .votes(
                round.getVotes() != null
                    ? new java.util.HashMap<>(round.getVotes())
                    : null)
            .isFinished(round.getIsFinished())
            .votingEndsAt(round.getVotingEndsAt());

    if (round.getPhase() == GameRound.RoundPhase.VOTING && round.getVotingEndsAt() != null) {
      long secondsLeft =
          ChronoUnit.SECONDS.between(Instant.now(), round.getVotingEndsAt());
      builder.votingTimeLeft((int) Math.max(0, secondsLeft));
    }

    return builder.build();
  }
}
