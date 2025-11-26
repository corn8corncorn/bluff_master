package com.bluffmaster.controller;

import com.bluffmaster.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/players/{playerId}/upload")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable String playerId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> imageUrls = imageService.uploadImages(playerId, files);
            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            // 讓 GlobalExceptionHandler 處理異常，這樣可以返回更詳細的錯誤信息
            throw new RuntimeException("上傳圖片失敗: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/players/{playerId}")
    public ResponseEntity<Void> deletePlayerImages(@PathVariable String playerId) {
        imageService.deletePlayerImages(playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/players/{playerId}/images")
    public ResponseEntity<Void> deletePlayerImage(
            @PathVariable String playerId,
            @RequestParam("imageUrl") String imageUrl) {
        try {
            imageService.deletePlayerImage(playerId, imageUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("刪除圖片失敗: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoomImages(@PathVariable String roomId) {
        imageService.deleteRoomImages(roomId);
        return ResponseEntity.ok().build();
    }
}

