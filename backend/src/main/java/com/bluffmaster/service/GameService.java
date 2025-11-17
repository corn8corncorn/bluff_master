package com.bluffmaster.service;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.model.GameRound;
import com.bluffmaster.model.Player;
import com.bluffmaster.model.Room;
import com.bluffmaster.repository.GameRoundRepository;
import com.bluffmaster.repository.PlayerRepository;
import com.bluffmaster.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final GameRoundRepository gameRoundRepository;
    private final ImageService imageService;

    @Value("${game.fake-image-url}")
    private String fakeImageUrl;

    @Transactional
    public GameRoundDTO startRound(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        if (room.getStatus() != Room.RoomStatus.PLAYING) {
            throw new RuntimeException("房間不在遊戲狀態");
        }

        List<Player> players = playerRepository.findByRoomId(roomId);
        List<Player> onlinePlayers = players.stream()
                .filter(Player::getIsOnline)
                .collect(Collectors.toList());

        if (onlinePlayers.isEmpty()) {
            throw new RuntimeException("沒有在線玩家");
        }

        // 選擇主講者（輪流）
        String speakerId = selectSpeaker(room, onlinePlayers);
        Player speaker = playerRepository.findById(speakerId)
                .orElseThrow(() -> new RuntimeException("主講者不存在"));

        // 抽取圖片
        List<String> allImages = collectAllImages(players, speakerId);
        Collections.shuffle(allImages);

        int realImageCount = room.getGameMode() == Room.GameMode.NORMAL ? 3 : 2;
        List<String> selectedImages = new ArrayList<>();
        
        // 選擇真圖
        for (int i = 0; i < Math.min(realImageCount, allImages.size()); i++) {
            selectedImages.add(allImages.get(i));
        }

        // 添加假圖
        selectedImages.add(fakeImageUrl);
        Collections.shuffle(selectedImages);

        // 創建回合
        GameRound round = GameRound.builder()
                .roomId(roomId)
                .roundNumber(room.getCurrentRound() + 1)
                .speakerId(speakerId)
                .imageUrls(selectedImages)
                .fakeImageUrl(fakeImageUrl)
                .isFinished(false)
                .build();

        round = gameRoundRepository.save(round);

        // 更新房間回合數
        room.setCurrentRound(round.getRoundNumber());
        roomRepository.save(room);

        return convertToDTO(round, speaker.getNickname());
    }

    @Transactional
    public void vote(String playerId, String imageUrl, String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            throw new RuntimeException("投票已結束");
        }

        if (round.getSpeakerId().equals(playerId)) {
            throw new RuntimeException("主講者不能投票");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (!player.getRoomId().equals(round.getRoomId())) {
            throw new RuntimeException("玩家不在該房間");
        }

        // 檢查圖片是否在選項中
        if (!round.getImageUrls().contains(imageUrl)) {
            throw new RuntimeException("無效的圖片選項");
        }

        round.getVotes().put(playerId, imageUrl);
        gameRoundRepository.save(round);
    }

    @Transactional
    public GameRoundDTO finishRound(String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            return convertToDTO(round, null);
        }

        Room room = roomRepository.findById(round.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        List<Player> players = playerRepository.findByRoomId(room.getId());
        Player speaker = playerRepository.findById(round.getSpeakerId())
                .orElseThrow(() -> new RuntimeException("主講者不存在"));

        // 計算分數
        int totalPlayers = players.size();
        int voters = players.size() - 1; // 排除主講者
        int correctVotes = 0;
        int wrongVotes = 0;
        int noVotes = 0;

        for (Player player : players) {
            if (player.getId().equals(round.getSpeakerId())) {
                continue; // 跳過主講者
            }

            String votedImage = round.getVotes().get(player.getId());
            if (votedImage == null) {
                // 未投票
                player.setScore(player.getScore() - 1);
                noVotes++;
            } else if (votedImage.equals(round.getFakeImageUrl())) {
                // 投中假圖
                player.setScore(player.getScore() + 1);
                correctVotes++;
            } else {
                // 投錯
                wrongVotes++;
            }
        }

        // 主講者分數 = ceil(投票者數 × 0.5)
        int speakerScore = (int) Math.ceil(voters * 0.5);
        speaker.setScore(speaker.getScore() + speakerScore);

        playerRepository.saveAll(players);

        round.setIsFinished(true);
        round = gameRoundRepository.save(round);

        // 檢查遊戲是否結束
        if (round.getRoundNumber() >= room.getTotalRounds()) {
            room.setStatus(Room.RoomStatus.FINISHED);
            roomRepository.save(room);
        }

        return convertToDTO(round, speaker.getNickname());
    }

    @Transactional
    public void handlePlayerDisconnect(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        player.setIsOnline(false);
        playerRepository.save(player);

        Room room = roomRepository.findById(player.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        // 如果主講者斷線，自動結束當前回合
        if (room.getStatus() == Room.RoomStatus.PLAYING) {
            Optional<GameRound> currentRound = gameRoundRepository.findByRoomIdAndIsFinishedFalse(room.getId());
            if (currentRound.isPresent() && currentRound.get().getSpeakerId().equals(playerId)) {
                finishRound(currentRound.get().getId());
            }
        }
    }

    @Transactional
    public void handlePlayerReconnect(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        player.setIsOnline(true);
        playerRepository.save(player);
    }

    private String selectSpeaker(Room room, List<Player> onlinePlayers) {
        // 簡單輪流機制：根據回合數選擇
        int roundIndex = room.getCurrentRound() % onlinePlayers.size();
        return onlinePlayers.get(roundIndex).getId();
    }

    private List<String> collectAllImages(List<Player> players, String excludePlayerId) {
        List<String> allImages = new ArrayList<>();
        for (Player player : players) {
            if (!player.getId().equals(excludePlayerId) && player.getImageUrls() != null) {
                allImages.addAll(player.getImageUrls());
            }
        }
        return allImages;
    }

    private GameRoundDTO convertToDTO(GameRound round, String speakerNickname) {
        return GameRoundDTO.builder()
                .id(round.getId())
                .roomId(round.getRoomId())
                .roundNumber(round.getRoundNumber())
                .speakerId(round.getSpeakerId())
                .speakerNickname(speakerNickname)
                .imageUrls(round.getImageUrls())
                .fakeImageUrl(round.getFakeImageUrl())
                .votes(round.getVotes())
                .isFinished(round.getIsFinished())
                .build();
    }
}

