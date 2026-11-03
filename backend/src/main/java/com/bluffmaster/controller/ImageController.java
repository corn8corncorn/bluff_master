package com.bluffmaster.controller;

import com.bluffmaster.dto.RoomDTO;
import com.bluffmaster.model.Player;
import com.bluffmaster.repository.PlayerRepository;
import com.bluffmaster.service.ImageService;
import com.bluffmaster.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/players/{playerId}/upload")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable String playerId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> imageUrls = imageService.uploadImages(playerId, files);

            Player player = playerRepository.findById(playerId).orElse(null);
            if (player != null) {
                RoomDTO room = roomService.getRoom(player.getRoomId());
                messagingTemplate.convertAndSend("/topic/room/" + player.getRoomId(), room);
            }

            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            throw new RuntimeException("上傳圖片失敗: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/players/{playerId}")
    public ResponseEntity<Void> deletePlayerImages(@PathVariable String playerId) {
        imageService.deletePlayerImages(playerId);
        broadcastRoom(playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/players/{playerId}/images")
    public ResponseEntity<Void> deletePlayerImage(
            @PathVariable String playerId,
            @RequestParam("imageUrl") String imageUrl) {
        try {
            imageService.deletePlayerImage(playerId, imageUrl);
            broadcastRoom(playerId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("刪除圖片失敗: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoomImages(@PathVariable String roomId) {
        imageService.deleteRoomImages(roomId);
        RoomDTO room = roomService.getRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, room);
        return ResponseEntity.ok().build();
    }

    private void broadcastRoom(String playerId) {
        playerRepository.findById(playerId).ifPresent(player -> {
            RoomDTO room = roomService.getRoom(player.getRoomId());
            messagingTemplate.convertAndSend("/topic/room/" + player.getRoomId(), room);
        });
    }
}
