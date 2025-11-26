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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${storage.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${game.max-image-size}")
    private long maxImageSize;

    @Value("${game.image-compression.min-width}")
    private int minWidth;

    @Value("${game.image-compression.max-width}")
    private int maxWidth;

    private Storage storage;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 確保上傳目錄存在
        if ("local".equals(storageType)) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("創建上傳目錄: {}", uploadPath.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("無法創建上傳目錄: {}", uploadDir, e);
            }
        }
    }

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
        log.info("開始上傳圖片，玩家ID: {}, 文件數量: {}", playerId, files.size());
        
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> {
                    log.error("玩家不存在: {}", playerId);
                    return new RuntimeException("玩家不存在");
                });

        // 檢查玩家是否已準備
        if (player.getIsReady()) {
            log.warn("玩家已準備，無法上傳圖片: {}", playerId);
            throw new RuntimeException("已準備狀態下無法上傳圖片，請先取消準備");
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                log.info("處理第 {} 個文件: {}, 大小: {} bytes, 類型: {}", 
                    i + 1, file.getOriginalFilename(), file.getSize(), file.getContentType());
                
                // 驗證文件大小
                if (file.getSize() > maxImageSize) {
                    log.error("圖片大小超過限制: {} bytes, 最大: {} bytes", file.getSize(), maxImageSize);
                    throw new RuntimeException("圖片大小超過 5MB 限制");
                }

                // 驗證文件類型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    log.error("不支援的文件類型: {}", contentType);
                    throw new RuntimeException("只支援圖片格式");
                }

                // 壓縮圖片
                log.debug("開始壓縮圖片: {}", file.getOriginalFilename());
                byte[] compressedImage = compressImage(file);
                log.debug("圖片壓縮完成，大小: {} bytes", compressedImage.length);

                // 生成 UUID 檔名
                String fileName = UUID.randomUUID().toString() + ".jpg";

                // 上傳圖片（根據配置選擇儲存方式）
                String imageUrl;
                if ("gcp".equals(storageType) && bucketName != null && !bucketName.isEmpty()) {
                    log.debug("使用 GCP 儲存");
                    imageUrl = uploadToGCP(fileName, compressedImage);
                } else {
                    // 本地開發模式：使用 Base64 或本地文件系統
                    log.debug("使用本地儲存");
                    imageUrl = uploadToLocal(fileName, compressedImage);
                }

                uploadedUrls.add(imageUrl);
                log.info("圖片上傳成功: {}", imageUrl);
            } catch (Exception e) {
                log.error("處理第 {} 個文件時發生錯誤: {}", i + 1, file.getOriginalFilename(), e);
                throw new RuntimeException("處理圖片失敗: " + e.getMessage(), e);
            }
        }

        // 更新玩家圖片列表
        if (player.getImageUrls() == null) {
            player.setImageUrls(new ArrayList<>());
        }
        player.getImageUrls().addAll(uploadedUrls);
        playerRepository.save(player);
        log.info("玩家圖片列表已更新，總共 {} 張圖片", player.getImageUrls().size());

        return uploadedUrls;
    }

    @Transactional
    public void deletePlayerImages(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (player.getImageUrls() != null) {
            for (String imageUrl : player.getImageUrls()) {
                if (imageUrl.startsWith("data:image") || imageUrl.startsWith("/api/images/") || imageUrl.startsWith("/uploads/")) {
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
    public void deletePlayerImage(String playerId, String imageUrl) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        // 檢查玩家是否已準備
        if (player.getIsReady()) {
            log.warn("玩家已準備，無法刪除圖片: {}", playerId);
            throw new RuntimeException("已準備狀態下無法刪除圖片，請先取消準備");
        }

        if (player.getImageUrls() == null || !player.getImageUrls().contains(imageUrl)) {
            throw new RuntimeException("圖片不存在");
        }

        // 刪除文件
        if (imageUrl.startsWith("data:image") || imageUrl.startsWith("/api/images/") || imageUrl.startsWith("/uploads/")) {
            deleteFromLocal(imageUrl);
        } else if (imageUrl.contains("storage.googleapis.com")) {
            deleteFromGCP(imageUrl);
        }

        // 從列表中移除
        player.getImageUrls().remove(imageUrl);
        playerRepository.save(player);
        log.info("已刪除玩家圖片: playerId={}, imageUrl={}", playerId, imageUrl);
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
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                log.error("無法讀取圖片，文件類型: {}", file.getContentType());
                throw new RuntimeException("無法讀取圖片，請確認文件格式正確");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            log.debug("原始圖片尺寸: {}x{}", originalWidth, originalHeight);

            // 計算新尺寸
            int newWidth = originalWidth;
            int newHeight = originalHeight;

            if (originalWidth > maxWidth) {
                newWidth = maxWidth;
                newHeight = (int) ((double) originalHeight * maxWidth / originalWidth);
                log.debug("圖片需要縮小到: {}x{}", newWidth, newHeight);
            } else if (originalWidth < minWidth) {
                newWidth = minWidth;
                newHeight = (int) ((double) originalHeight * minWidth / originalWidth);
                log.debug("圖片需要放大到: {}x{}", newWidth, newHeight);
            }

            // 調整圖片大小
            java.awt.Image scaledImage = originalImage.getScaledInstance(
                    newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
            BufferedImage bufferedScaledImage = new BufferedImage(
                    newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            
            java.awt.Graphics2D g2d = bufferedScaledImage.createGraphics();
            try {
                g2d.drawImage(scaledImage, 0, 0, null);
            } finally {
                g2d.dispose();
            }

            // 轉換為 byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(bufferedScaledImage, "jpg", baos);
            if (!written) {
                log.error("無法將圖片寫入輸出流");
                throw new RuntimeException("圖片處理失敗");
            }
            
            byte[] result = baos.toByteArray();
            log.debug("圖片壓縮完成，輸出大小: {} bytes", result.length);
            return result;
        } catch (IOException e) {
            log.error("壓縮圖片時發生 IO 錯誤", e);
            throw new RuntimeException("圖片處理失敗: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("壓縮圖片時發生未知錯誤", e);
            throw new RuntimeException("圖片處理失敗: " + e.getMessage(), e);
        }
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
            // 本地開發模式：保存到文件系統
            Path uploadPath = Paths.get(uploadDir);
            Path filePath = uploadPath.resolve(fileName);
            
            // 確保目錄存在
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 寫入文件
            Files.write(filePath, imageData);
            log.info("圖片已保存到: {}", filePath.toAbsolutePath());
            
            // 返回相對路徑 URL（前端可以通過 /api/images/ 訪問）
            return "/api/images/" + fileName;
        } catch (Exception e) {
            log.error("本地儲存圖片失敗", e);
            throw new RuntimeException("上傳圖片失敗", e);
        }
    }

    private void deleteFromLocal(String imageUrl) {
        try {
            // 從 URL 中提取文件名
            // 支持格式: /api/images/{fileName} 或 /uploads/{fileName} 或 data:image/jpeg;base64,...
            String fileName;
            if (imageUrl.startsWith("data:image")) {
                // Base64 格式，無法刪除文件（舊格式，保留兼容性）
                log.warn("嘗試刪除 Base64 格式圖片，跳過文件刪除: {}", imageUrl);
                return;
            } else if (imageUrl.startsWith("/api/images/")) {
                fileName = imageUrl.substring("/api/images/".length());
            } else if (imageUrl.startsWith("/uploads/")) {
                fileName = imageUrl.substring("/uploads/".length());
            } else {
                fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            }
            
            Path filePath = Paths.get(uploadDir, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("已刪除本地圖片: {}", filePath.toAbsolutePath());
            } else {
                log.warn("嘗試刪除不存在的圖片文件: {}", filePath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("刪除本地圖片失敗: " + imageUrl, e);
            throw new RuntimeException("刪除圖片失敗: " + e.getMessage(), e);
        }
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

