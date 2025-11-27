package com.bluffmaster.service;

import com.bluffmaster.dto.GameRoundDTO;
import com.bluffmaster.dto.VoteResult;
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

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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

        // 檢查主講者是否有足夠的圖片
        if (speaker.getImageUrls() == null || speaker.getImageUrls().size() < 3) {
            throw new RuntimeException("主講者需要至少上傳 3 張圖片");
        }

        // 從主講者的圖片中隨機選3張
        List<String> speakerImages = new ArrayList<>(speaker.getImageUrls());
        Collections.shuffle(speakerImages);
        List<String> selectedImages = new ArrayList<>();
        
        // 選擇3張主講者的圖片
        for (int i = 0; i < 3; i++) {
            selectedImages.add(speakerImages.get(i));
        }

        // 生成固定的假圖 URL（使用隨機 ID 確保每個回合的假圖不同，但同一回合所有玩家看到相同）
        // 嘗試多個 ID 直到找到有效的圖片
        Random random = new Random();
        String fixedFakeImageUrl = null;
        int maxAttempts = 10;
        int attempts = 0;
        
        while (fixedFakeImageUrl == null && attempts < maxAttempts) {
            int fakeImageId = random.nextInt(1000) + 1; // 1-1000
            String initialFakeImageUrl = String.format("https://picsum.photos/id/%d/800/600", fakeImageId);
            
            // 獲取假圖的完整 URL（包括所有參數），並驗證圖片是否存在
            String candidateUrl = getFinalImageUrl(initialFakeImageUrl);
            if (candidateUrl != null && validateImageUrl(candidateUrl)) {
                fixedFakeImageUrl = candidateUrl;
                log.info("假圖 URL - 初始: {}, 最終: {}", initialFakeImageUrl, fixedFakeImageUrl);
                break; // 找到有效的圖片，跳出循環
            } else {
                log.warn("假圖 URL 無效，重試中: {} (attempt {}/{})", candidateUrl, attempts + 1, maxAttempts);
                attempts++;
            }
        }
        
        // 如果所有嘗試都失敗，使用備用 URL
        if (fixedFakeImageUrl == null) {
            fixedFakeImageUrl = "https://picsum.photos/id/29/800/600.jpg?hmac=KbDC2qhBvoFM4XpOZrAnybO4JNiXS9mox0PORy6NCJA";
            log.warn("所有假圖 URL 都無效，使用備用 URL: {}", fixedFakeImageUrl);
        }
        
        // 添加1張假圖（非所有玩家的圖片）
        selectedImages.add(fixedFakeImageUrl);
        Collections.shuffle(selectedImages);

        // 創建回合
        GameRound round = GameRound.builder()
                .roomId(roomId)
                .roundNumber(room.getCurrentRound() + 1)
                .speakerId(speakerId)
                .imageUrls(selectedImages)
                .fakeImageUrl(fixedFakeImageUrl)  // 使用固定的假圖 URL
                .phase(GameRound.RoundPhase.STORY_TELLING)
                .isFinished(false)
                .build();

        round = gameRoundRepository.save(round);

        // 更新房間回合數
        room.setCurrentRound(round.getRoundNumber());
        roomRepository.save(room);

        return convertToDTO(round, speaker.getNickname());
    }

    public GameRoundDTO getCurrentRound(String roomId) {
        Optional<GameRound> currentRound = gameRoundRepository.findByRoomIdAndIsFinishedFalse(roomId);
        if (currentRound.isPresent()) {
            GameRound round = currentRound.get();
            Player speaker = playerRepository.findById(round.getSpeakerId())
                    .orElseThrow(() -> new RuntimeException("主講者不存在"));
            return convertToDTO(round, speaker.getNickname());
        }
        return null;
    }

    @Transactional
    public void vote(String playerId, String imageUrl, String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            throw new RuntimeException("投票已結束");
        }

        if (round.getPhase() != GameRound.RoundPhase.VOTING) {
            throw new RuntimeException("當前階段不能投票");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (!player.getRoomId().equals(round.getRoomId())) {
            throw new RuntimeException("玩家不在該房間");
        }

        // 如果是主講者，選擇說謊圖片
        if (round.getSpeakerId().equals(playerId)) {
            // 檢查圖片是否在主講者的圖片中
            Player speaker = playerRepository.findById(round.getSpeakerId())
                    .orElseThrow(() -> new RuntimeException("主講者不存在"));
            if (!speaker.getImageUrls().contains(imageUrl)) {
                throw new RuntimeException("無效的圖片選項");
            }
            round.setSpeakerFakeImageUrl(imageUrl);
        } else {
            // 其他玩家投票
            // 檢查圖片是否在選項中
            if (!round.getImageUrls().contains(imageUrl)) {
                throw new RuntimeException("無效的圖片選項");
            }
            round.getVotes().put(playerId, imageUrl);
        }

        gameRoundRepository.save(round);
    }

    @Transactional
    public GameRoundDTO startVoting(String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            throw new RuntimeException("回合已結束");
        }

        if (round.getPhase() != GameRound.RoundPhase.QUESTIONING) {
            throw new RuntimeException("當前階段不能開始投票");
        }

        round.setPhase(GameRound.RoundPhase.VOTING);
        round = gameRoundRepository.save(round);

        // 強制加載懶加載的集合，避免 LazyInitializationException
        if (round.getImageUrls() != null) {
            round.getImageUrls().size(); // 觸發加載
        }
        if (round.getVotes() != null) {
            round.getVotes().size(); // 觸發加載
        }

        Player speaker = playerRepository.findById(round.getSpeakerId())
                .orElseThrow(() -> new RuntimeException("主講者不存在"));
        return convertToDTO(round, speaker.getNickname());
    }

    @Transactional
    public GameRoundDTO revealResult(String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            // 強制加載懶加載的集合
            if (round.getImageUrls() != null) {
                round.getImageUrls().size();
            }
            if (round.getVotes() != null) {
                round.getVotes().size();
            }
            return convertToDTO(round, null);
        }

        if (round.getPhase() != GameRound.RoundPhase.VOTING) {
            throw new RuntimeException("當前階段不能公布結果");
        }

        round.setPhase(GameRound.RoundPhase.REVEALING);
        round = gameRoundRepository.save(round);

        // 強制加載懶加載的集合，避免 LazyInitializationException
        if (round.getImageUrls() != null) {
            round.getImageUrls().size(); // 觸發加載
        }
        if (round.getVotes() != null) {
            round.getVotes().size(); // 觸發加載
        }

        Room room = roomRepository.findById(round.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));
        List<Player> players = playerRepository.findByRoomId(room.getId());
        Player speaker = playerRepository.findById(round.getSpeakerId())
                .orElseThrow(() -> new RuntimeException("主講者不存在"));

        // 計算投票結果（但不保存分數，只是用於顯示）
        String correctFakeImage = round.getSpeakerFakeImageUrl() != null 
                ? round.getSpeakerFakeImageUrl() 
                : round.getFakeImageUrl();
        
        int voters = players.size() - 1; // 排除主講者
        int correctVotes = 0;
        int actualVoters = 0; // 實際投票的玩家數（不包括未投票的）
        Map<String, VoteResult> voteResults = new HashMap<>();

        // 第一遍遍歷：處理非主講者玩家，統計猜中次數
        for (Player player : players) {
            if (player.getId().equals(round.getSpeakerId())) {
                continue; // 跳過主講者，稍後處理
            }

            String votedImage = round.getVotes().get(player.getId());
            int scoreChange = 0;
            boolean isCorrect = false;
            
            if (votedImage == null) {
                // 未投票
                scoreChange = -1;
            } else {
                actualVoters++; // 有投票的玩家
                if (votedImage.equals(correctFakeImage)) {
                    // 投中假圖
                    scoreChange = 1;
                    isCorrect = true;
                    correctVotes++;
                } else {
                    // 投錯
                    scoreChange = 0;
                }
            }
            
            VoteResult result = VoteResult.builder()
                    .playerId(player.getId())
                    .playerNickname(player.getNickname())
                    .votedImageUrl(votedImage)
                    .isCorrect(isCorrect)
                    .scoreChange(scoreChange)
                    .isSpeaker(false)
                    .build();
            voteResults.put(player.getId(), result);
        }

        // 處理主講者分數（不保存，僅顯示）
        int speakerScore;
        if (correctVotes == 0) {
            // 沒有任何一個玩家猜中，主講者+3分
            speakerScore = 3;
        } else if (actualVoters > 0 && correctVotes == actualVoters) {
            // 所有投票的玩家都猜中了，主講者-2分
            speakerScore = -2;
        } else {
            // 其他情況：固定得分 = ceil(投票者數 × 0.5)
            speakerScore = (int) Math.ceil(voters * 0.5);
        }
        
        VoteResult speakerResult = VoteResult.builder()
                .playerId(speaker.getId())
                .playerNickname(speaker.getNickname())
                .votedImageUrl(round.getSpeakerFakeImageUrl())
                .isCorrect(true)  // 主講者選擇的圖片就是正確答案
                .scoreChange(speakerScore)
                .isSpeaker(true)
                .build();
        voteResults.put(speaker.getId(), speakerResult);

        GameRoundDTO dto = convertToDTO(round, speaker.getNickname());
        dto.setVoteResults(voteResults);
        return dto;
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

        // 使用主講者選擇的說謊圖片作為正確答案
        String correctFakeImage = round.getSpeakerFakeImageUrl() != null 
                ? round.getSpeakerFakeImageUrl() 
                : round.getFakeImageUrl();

        // 計算分數並記錄每個玩家的投票結果
        int voters = players.size() - 1; // 排除主講者
        int correctVotes = 0;
        int actualVoters = 0; // 實際投票的玩家數（不包括未投票的）
        
        Map<String, VoteResult> voteResults = new HashMap<>();

        // 第一遍遍歷：處理非主講者玩家，統計猜中次數
        for (Player player : players) {
            if (player.getId().equals(round.getSpeakerId())) {
                continue; // 跳過主講者，稍後處理
            }

            String votedImage = round.getVotes().get(player.getId());
            int scoreChange = 0;
            boolean isCorrect = false;
            
            if (votedImage == null) {
                // 未投票
                player.setScore(player.getScore() - 1);
                scoreChange = -1;
            } else {
                actualVoters++; // 有投票的玩家
                if (votedImage.equals(correctFakeImage)) {
                    // 投中假圖
                    player.setScore(player.getScore() + 1);
                    scoreChange = 1;
                    isCorrect = true;
                    correctVotes++;
                } else {
                    // 投錯
                    scoreChange = 0;
                }
            }
            
            VoteResult result = VoteResult.builder()
                    .playerId(player.getId())
                    .playerNickname(player.getNickname())
                    .votedImageUrl(votedImage)
                    .isCorrect(isCorrect)
                    .scoreChange(scoreChange)
                    .isSpeaker(false)
                    .build();
            voteResults.put(player.getId(), result);
        }

        // 處理主講者分數
        int speakerScore;
        
        if (correctVotes == 0) {
            // 沒有任何一個玩家猜中，主講者+3分
            speakerScore = 3;
        } else if (actualVoters > 0 && correctVotes == actualVoters) {
            // 所有投票的玩家都猜中了，主講者-2分
            speakerScore = -2;
        } else {
            // 其他情況：固定得分 = ceil(投票者數 × 0.5)
            speakerScore = (int) Math.ceil(voters * 0.5);
        }
        
        speaker.setScore(speaker.getScore() + speakerScore);
        
        VoteResult speakerResult = VoteResult.builder()
                .playerId(speaker.getId())
                .playerNickname(speaker.getNickname())
                .votedImageUrl(round.getSpeakerFakeImageUrl())
                .isCorrect(true)  // 主講者選擇的圖片就是正確答案
                .scoreChange(speakerScore)
                .isSpeaker(true)
                .build();
        voteResults.put(speaker.getId(), speakerResult);

        playerRepository.saveAll(players);

        round.setPhase(GameRound.RoundPhase.FINISHED);
        round.setIsFinished(true);
        round = gameRoundRepository.save(round);

        // 檢查遊戲是否結束
        if (round.getRoundNumber() >= room.getTotalRounds()) {
            room.setStatus(Room.RoomStatus.FINISHED);
            roomRepository.save(room);
        }

        // 將投票結果存儲到回合中（通過一個臨時字段，實際存儲在 DTO 中）
        GameRoundDTO dto = convertToDTO(round, speaker.getNickname());
        dto.setVoteResults(voteResults);
        return dto;
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
        // 按照加入順序排序：房主先開始，然後按加入時間（createdAt）排序
        List<Player> sortedPlayers = onlinePlayers.stream()
                .sorted((p1, p2) -> {
                    // 房主優先
                    if (p1.getIsHost() && !p2.getIsHost()) {
                        return -1;
                    }
                    if (!p1.getIsHost() && p2.getIsHost()) {
                        return 1;
                    }
                    // 如果都是房主或都不是房主，按創建時間排序
                    return p1.getCreatedAt().compareTo(p2.getCreatedAt());
                })
                .collect(Collectors.toList());
        
        // 根據回合數選擇（第1輪是房主，第2輪是第一個加入的玩家，以此類推）
        int roundIndex = room.getCurrentRound() % sortedPlayers.size();
        return sortedPlayers.get(roundIndex).getId();
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

    @Transactional
    public GameRoundDTO nextPhase(String roundId) {
        GameRound round = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("回合不存在"));

        if (round.getIsFinished()) {
            throw new RuntimeException("回合已結束");
        }

        // 階段轉換：STORY_TELLING -> QUESTIONING -> VOTING
        switch (round.getPhase()) {
            case STORY_TELLING:
                round.setPhase(GameRound.RoundPhase.QUESTIONING);
                break;
            case QUESTIONING:
                // 應該使用 startVoting 方法
                throw new RuntimeException("請使用開始投票接口");
            case VOTING:
                // 應該使用 revealResult 方法
                throw new RuntimeException("請使用公布結果接口");
            default:
                throw new RuntimeException("當前階段不能轉換");
        }

        round = gameRoundRepository.save(round);
        
        // 強制加載懶加載的集合，避免 LazyInitializationException
        if (round.getImageUrls() != null) {
            round.getImageUrls().size(); // 觸發加載
        }
        if (round.getVotes() != null) {
            round.getVotes().size(); // 觸發加載
        }
        
        Player speaker = playerRepository.findById(round.getSpeakerId())
                .orElseThrow(() -> new RuntimeException("主講者不存在"));
        return convertToDTO(round, speaker.getNickname());
    }

    /**
     * 獲取圖片的最終 URL（跟隨所有重定向，獲取包含所有參數的完整 URL）
     * 這樣可以確保所有玩家看到相同的圖片
     */
    private String getFinalImageUrl(String initialUrl) {
        String currentUrl = initialUrl;
        int maxRedirects = 10; // 最多跟隨10次重定向
        int redirectCount = 0;
        
        try {
            while (redirectCount < maxRedirects) {
                HttpURLConnection connection = (HttpURLConnection) new URL(currentUrl).openConnection();
                connection.setInstanceFollowRedirects(false); // 不自動跟隨重定向
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000); // 5秒超時
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                int responseCode = connection.getResponseCode();
                
                // 如果返回重定向（301, 302, 303, 307, 308），獲取 Location header
                if (responseCode >= 300 && responseCode < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        // 如果是相對路徑，轉換為絕對路徑
                        if (location.startsWith("/")) {
                            URI baseUri = new URI(currentUrl);
                            URI locationUri = baseUri.resolve(location);
                            currentUrl = locationUri.toString();
                        } else if (location.startsWith("http://") || location.startsWith("https://")) {
                            currentUrl = location;
                        } else {
                            // 相對路徑，需要解析
                            URI baseUri = new URI(currentUrl);
                            URI locationUri = baseUri.resolve(location);
                            currentUrl = locationUri.toString();
                        }
                        redirectCount++;
                        connection.disconnect();
                        continue;
                    }
                }
                
                // 檢查響應碼，200-299 表示成功
                if (responseCode >= 200 && responseCode < 300) {
                    connection.disconnect();
                    return currentUrl;
                } else {
                    // 響應碼不是成功，返回 null 表示無效
                    connection.disconnect();
                    return null;
                }
            }
            
            // 如果超過最大重定向次數，返回 null 表示無效
            log.warn("跟隨重定向超過最大次數: {}", currentUrl);
            return null;
        } catch (Exception e) {
            log.warn("獲取假圖最終 URL 失敗: {}", initialUrl, e);
            // 如果獲取失敗，返回 null 表示無效
            return null;
        }
    }
    
    /**
     * 驗證圖片 URL 是否有效（圖片是否存在）
     */
    private boolean validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }
        
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5秒超時
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            // 200-299 表示成功，404 表示圖片不存在
            if (responseCode == 404) {
                log.warn("圖片不存在 (404): {}", imageUrl);
                return false;
            }
            
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            log.warn("驗證圖片 URL 失敗: {}", imageUrl, e);
            return false;
        }
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
                .speakerFakeImageUrl(round.getSpeakerFakeImageUrl())
                .phase(round.getPhase())
                .votes(round.getVotes())
                .isFinished(round.getIsFinished())
                .build();
    }
}

