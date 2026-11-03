package com.bluffmaster.service;

import com.bluffmaster.dto.FakeThemeOptionsDTO;
import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 假圖庫：選主題時從 Wikimedia / Unsplash 即時抓圖，快取至 uploads/fake/{region}/{category}/。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FakeImageService {

  private final ExternalFakeImageFetcher externalFakeImageFetcher;
  private final Random random = new Random();

  @Value("${storage.upload-dir:./uploads}")
  private String uploadDir;

  @PostConstruct
  public void init() {
    log.info("假圖服務：選主題時從 Wikimedia/Unsplash 即時抓圖並快取至本地");
  }

  public FakeThemeOptionsDTO getThemeOptions() {
    List<FakeThemeOptionsDTO.ThemeOption> regions =
        Arrays.stream(FakeImageRegion.values())
            .filter(r -> r != FakeImageRegion.FOREIGN)
            .map(
                r ->
                    FakeThemeOptionsDTO.ThemeOption.builder()
                        .id(r.name())
                        .label(r.getLabel())
                        .build())
            .collect(Collectors.toList());

    List<FakeThemeOptionsDTO.ThemeOption> categories =
        Arrays.stream(FakeImageCategory.values())
            .map(
                c ->
                    FakeThemeOptionsDTO.ThemeOption.builder()
                        .id(c.name())
                        .label(c.getLabel())
                        .build())
            .collect(Collectors.toList());

    return FakeThemeOptionsDTO.builder().regions(regions).categories(categories).build();
  }

  public String pickRandom(FakeImageRegion region, FakeImageCategory category) {
    Path themeDir = themeDirectory(region, category);

    for (int attempt = 0; attempt < 3; attempt++) {
      Optional<String> fetched =
          externalFakeImageFetcher.fetchAndSave(region, category, themeDir);
      if (fetched.isPresent()) {
        return toPublicUrl(region, category, fetched.get());
      }
    }

    List<String> cached = listCachedWebImages(themeDir, region, category);
    if (!cached.isEmpty()) {
      return cached.get(random.nextInt(cached.size()));
    }

    throw new RuntimeException("無法取得主題圖片，請確認伺服器可連線至網際網路後再試");
  }

  public String getThemeLabel(FakeImageRegion region, FakeImageCategory category) {
    return region.getLabel() + " · " + category.getLabel();
  }

  private Path themeDirectory(FakeImageRegion region, FakeImageCategory category) {
    return Paths.get(
        uploadDir,
        "fake",
        region.name().toLowerCase(),
        category.name().toLowerCase());
  }

  private List<String> listCachedWebImages(
      Path themeDir, FakeImageRegion region, FakeImageCategory category) {
    if (!Files.isDirectory(themeDir)) {
      return List.of();
    }
    try (Stream<Path> paths = Files.list(themeDir)) {
      return paths
          .filter(Files::isRegularFile)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(name -> name.toLowerCase().startsWith("web_"))
          .filter(this::isImageFileName)
          .map(name -> toPublicUrl(region, category, name))
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.warn("讀取假圖快取失敗: {}", themeDir, e);
      return List.of();
    }
  }

  private String toPublicUrl(FakeImageRegion region, FakeImageCategory category, String fileName) {
    return "/api/images/fake/"
        + region.name().toLowerCase()
        + "/"
        + category.name().toLowerCase()
        + "/"
        + fileName;
  }

  private boolean isImageFileName(String name) {
    String lower = name.toLowerCase();
    return lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".png")
        || lower.endsWith(".webp");
  }
}
