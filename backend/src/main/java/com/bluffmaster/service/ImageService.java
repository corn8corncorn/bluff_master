package com.bluffmaster.service;

import com.bluffmaster.model.Player;
import com.bluffmaster.repository.PlayerRepository;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final PlayerRepository playerRepository;

    @Value("${gcp.storage.bucket-name:}")
    private String bucketName;

    @Value("${gcp.storage.project-id:}")
    private String projectId;

    @Value("${gcp.storage.credentials-path:}")
    private String credentialsPath;

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${game.max-image-size}")
    private long maxImageSize;

    @Value("${game.image-compression.min-width}")
    private int minWidth;

    @Value("${game.image-compression.max-width}")
    private int maxWidth;

    private Storage storage;

    private Storage getStorage() {
        if (storage == null && bucketName != null && !bucketName.isEmpty()) {
            try {
                if (credentialsPath != null && !credentialsPath.isEmpty()) {
                    storage = StorageOptions.newBuilder()
                            .setProjectId(projectId)
                            .build()
                            .getService();
                } else {
                    // 使用默認憑證
                    storage = StorageOptions.getDefaultInstance().getService();
                }
            } catch (Exception e) {
                log.warn("無法初始化 GCP Storage，將使用本地模式: " + e.getMessage());
            }
        }
        return storage;
    }

    @Transactional
    public List<String> uploadImages(String playerId, List<MultipartFile> files) throws IOException {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // 驗證文件大小
            if (file.getSize() > maxImageSize) {
                throw new RuntimeException("圖片大小超過 5MB 限制");
            }

            // 驗證文件類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("只支援圖片格式");
            }

            // 壓縮圖片
            byte[] compressedImage = compressImage(file);

            // 生成 UUID 檔名
            String fileName = UUID.randomUUID().toString() + ".jpg";

            // 上傳圖片（根據配置選擇儲存方式）
            String imageUrl;
            if ("gcp".equals(storageType) && bucketName != null && !bucketName.isEmpty()) {
                imageUrl = uploadToGCP(fileName, compressedImage);
            } else {
                // 本地開發模式：使用 Base64 或本地文件系統
                imageUrl = uploadToLocal(fileName, compressedImage);
            }

            uploadedUrls.add(imageUrl);
        }

        // 更新玩家圖片列表
        if (player.getImageUrls() == null) {
            player.setImageUrls(new ArrayList<>());
        }
        player.getImageUrls().addAll(uploadedUrls);
        playerRepository.save(player);

        return uploadedUrls;
    }

    @Transactional
    public void deletePlayerImages(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (player.getImageUrls() != null) {
            for (String imageUrl : player.getImageUrls()) {
                if (imageUrl.startsWith("data:image") || imageUrl.startsWith("/uploads/")) {
                    deleteFromLocal(imageUrl);
                } else if (imageUrl.contains("storage.googleapis.com")) {
                    deleteFromGCP(imageUrl);
                }
            }
            player.getImageUrls().clear();
            playerRepository.save(player);
        }
    }

    @Transactional
    public void deleteRoomImages(String roomId) {
        List<Player> players = playerRepository.findByRoomId(roomId);
        for (Player player : players) {
            deletePlayerImages(player.getId());
        }
    }

    @Scheduled(fixedRate = 3600000) // 每小時檢查一次
    @Transactional
    public void cleanupOldImages() {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        // 清理超過 3 小時的房間圖片
        // 這裡可以根據實際需求實現清理邏輯
        log.info("執行圖片清理任務");
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new RuntimeException("無法讀取圖片");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 計算新尺寸
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > maxWidth) {
            newWidth = maxWidth;
            newHeight = (int) ((double) originalHeight * maxWidth / originalWidth);
        } else if (originalWidth < minWidth) {
            newWidth = minWidth;
            newHeight = (int) ((double) originalHeight * minWidth / originalWidth);
        }

        // 調整圖片大小
        java.awt.Image scaledImage = originalImage.getScaledInstance(
                newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage bufferedScaledImage = new BufferedImage(
                newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        bufferedScaledImage.getGraphics().drawImage(scaledImage, 0, 0, null);

        // 轉換為 byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedScaledImage, "jpg", baos);
        return baos.toByteArray();
    }

    private String uploadToGCP(String fileName, byte[] imageData) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/jpeg")
                    .build();
            
            getStorage().create(blobInfo, imageData);
            
            // 返回公開 URL（需要設置 bucket 為公開或使用簽名 URL）
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
        } catch (Exception e) {
            log.error("上傳圖片到 GCP 失敗", e);
            throw new RuntimeException("上傳圖片失敗", e);
        }
    }

    private String uploadToLocal(String fileName, byte[] imageData) {
        try {
            // 本地開發模式：使用 Base64 編碼直接返回
            // 這樣可以避免需要配置文件系統路徑
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            return "data:image/jpeg;base64," + base64Image;
        } catch (Exception e) {
            log.error("本地儲存圖片失敗", e);
            throw new RuntimeException("上傳圖片失敗", e);
        }
    }

    private void deleteFromLocal(String imageUrl) {
        // Base64 圖片不需要刪除，因為是內嵌在 URL 中
        // 如果是文件系統路徑，可以在這裡實現刪除邏輯
        log.debug("刪除本地圖片: " + imageUrl);
    }

    private void deleteFromGCP(String imageUrl) {
        try {
            if (bucketName == null || bucketName.isEmpty()) {
                return;
            }
            // 從 URL 中提取檔案名
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            BlobId blobId = BlobId.of(bucketName, fileName);
            getStorage().delete(blobId);
        } catch (Exception e) {
            log.error("從 GCP 刪除圖片失敗: " + imageUrl, e);
        }
    }
}

