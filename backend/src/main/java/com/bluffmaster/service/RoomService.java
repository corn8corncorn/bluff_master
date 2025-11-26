package com.bluffmaster.service;

import com.bluffmaster.dto.*;
import com.bluffmaster.model.Player;
import com.bluffmaster.model.Room;
import com.bluffmaster.repository.PlayerRepository;
import com.bluffmaster.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public RoomDTO createRoom(CreateRoomRequest request) {
        // 生成房間代碼
        String roomCode = generateRoomCode();
        
        // 創建房間
        Room room = Room.builder()
                .roomCode(roomCode)
                .maxPlayers(request.getMaxPlayers())
                .gameMode(request.getGameMode())
                .status(Room.RoomStatus.WAITING)
                .currentRound(0)
                .totalRounds(request.getGameMode() == Room.GameMode.NORMAL 
                        ? request.getMaxPlayers() 
                        : Math.min(5, request.getMaxPlayers()))
                .build();
        
        room = roomRepository.save(room);

        // 創建房主
        Player host = Player.builder()
                .nickname(request.getNickname())
                .roomId(room.getId())
                .isHost(true)
                .isReady(false)
                .score(0)
                .isOnline(true)
                .build();
        
        host = playerRepository.save(host);

        // 更新房間玩家列表
        room.getPlayerIds().add(host.getId());
        room = roomRepository.save(room);

        return convertToDTO(room, host.getId());
    }

    @Transactional
    public RoomDTO joinRoom(JoinRoomRequest request) {
        Room room = roomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new RuntimeException("遊戲已開始，無法加入");
        }

        if (room.getPlayerIds().size() >= room.getMaxPlayers()) {
            throw new RuntimeException("房間已滿");
        }

        // 檢查暱稱是否重複
        boolean nicknameExists = playerRepository.findByRoomId(room.getId()).stream()
                .anyMatch(p -> p.getNickname().equals(request.getNickname()));
        if (nicknameExists) {
            throw new RuntimeException("暱稱已被使用");
        }

        // 創建玩家
        Player player = Player.builder()
                .nickname(request.getNickname())
                .roomId(room.getId())
                .isHost(false)
                .isReady(false)
                .score(0)
                .isOnline(true)
                .build();
        
        player = playerRepository.save(player);

        // 更新房間玩家列表
        room.getPlayerIds().add(player.getId());
        room = roomRepository.save(room);

        return convertToDTO(room, null);
    }

    @Transactional
    public void playerReady(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));
        
        player.setIsReady(true);
        playerRepository.save(player);
    }

    @Transactional
    public void updatePlayerNickname(String playerId, String newNickname) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));
        
        Room room = roomRepository.findById(player.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new RuntimeException("遊戲已開始，無法修改暱稱");
        }

        // 檢查暱稱是否重複
        boolean nicknameExists = playerRepository.findByRoomId(room.getId()).stream()
                .anyMatch(p -> !p.getId().equals(playerId) && p.getNickname().equals(newNickname));
        if (nicknameExists) {
            throw new RuntimeException("暱稱已被使用");
        }

        player.setNickname(newNickname);
        playerRepository.save(player);
    }

    @Transactional
    public void kickPlayer(String hostId, String targetPlayerId) {
        Player host = playerRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("房主不存在"));
        
        if (!host.getIsHost()) {
            throw new RuntimeException("只有房主可以踢人");
        }

        Player target = playerRepository.findById(targetPlayerId)
                .orElseThrow(() -> new RuntimeException("目標玩家不存在"));

        if (!target.getRoomId().equals(host.getRoomId())) {
            throw new RuntimeException("玩家不在同一房間");
        }

        Room room = roomRepository.findById(host.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new RuntimeException("遊戲已開始，無法踢人");
        }

        // 移除玩家
        room.getPlayerIds().remove(targetPlayerId);
        roomRepository.save(room);
        playerRepository.delete(target);
    }

    @Transactional
    public void startGame(String hostId) {
        Player host = playerRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("房主不存在"));
        
        if (!host.getIsHost()) {
            throw new RuntimeException("只有房主可以開始遊戲");
        }

        Room room = roomRepository.findById(host.getRoomId())
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new RuntimeException("遊戲已開始");
        }

        List<Player> players = playerRepository.findByRoomId(room.getId());
        
        if (players.size() < 4) {
            throw new RuntimeException("至少需要 4 人才能開始遊戲");
        }

        // 檢查所有玩家是否都準備好
        boolean allReady = players.stream().allMatch(Player::getIsReady);
        if (!allReady) {
            throw new RuntimeException("還有玩家未準備");
        }

        // 檢查圖片上傳數量
        for (Player player : players) {
            int requiredImages = room.getGameMode() == Room.GameMode.NORMAL 
                    ? room.getMaxPlayers() + 2 
                    : 5;
            
            if (player.getImageUrls() == null || player.getImageUrls().size() < requiredImages) {
                throw new RuntimeException("玩家 " + player.getNickname() + " 圖片數量不足");
            }
        }

        room.setStatus(Room.RoomStatus.PLAYING);
        room.setStartedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);
    }

    public RoomDTO getRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房間不存在"));
        
        String hostId = playerRepository.findByRoomId(roomId).stream()
                .filter(Player::getIsHost)
                .map(Player::getId)
                .findFirst()
                .orElse(null);
        
        return convertToDTO(room, hostId);
    }

    public RoomDTO getRoomByCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("房間不存在"));
        
        String hostId = playerRepository.findByRoomId(room.getId()).stream()
                .filter(Player::getIsHost)
                .map(Player::getId)
                .findFirst()
                .orElse(null);
        
        return convertToDTO(room, hostId);
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private RoomDTO convertToDTO(Room room, String hostId) {
        List<Player> players = playerRepository.findByRoomId(room.getId());
        List<PlayerDTO> playerDTOs = players.stream()
                .map(this::convertPlayerToDTO)
                .collect(Collectors.toList());

        return RoomDTO.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .maxPlayers(room.getMaxPlayers())
                .gameMode(room.getGameMode())
                .status(room.getStatus())
                .players(playerDTOs)
                .currentRound(room.getCurrentRound())
                .totalRounds(room.getTotalRounds())
                .hostId(hostId != null ? hostId : 
                        players.stream()
                                .filter(Player::getIsHost)
                                .map(Player::getId)
                                .findFirst()
                                .orElse(null))
                .build();
    }

    private PlayerDTO convertPlayerToDTO(Player player) {
        return PlayerDTO.builder()
                .id(player.getId())
                .nickname(player.getNickname())
                .roomId(player.getRoomId())
                .isHost(player.getIsHost())
                .isReady(player.getIsReady())
                .score(player.getScore())
                .imageUrls(player.getImageUrls())
                .isOnline(player.getIsOnline())
                .build();
    }
}

