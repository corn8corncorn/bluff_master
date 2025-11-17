package com.bluffmaster.controller;

import com.bluffmaster.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/players/{playerId}/upload")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable String playerId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> imageUrls = imageService.uploadImages(playerId, files);
        return ResponseEntity.ok(imageUrls);
    }

    @DeleteMapping("/players/{playerId}")
    public ResponseEntity<Void> deletePlayerImages(@PathVariable String playerId) {
        imageService.deletePlayerImages(playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoomImages(@PathVariable String roomId) {
        imageService.deleteRoomImages(roomId);
        return ResponseEntity.ok().build();
    }
}

