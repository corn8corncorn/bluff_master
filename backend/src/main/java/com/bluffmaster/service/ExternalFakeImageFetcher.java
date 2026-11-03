package com.bluffmaster.service;

import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 依主題從 Openverse / Wikimedia / Unsplash 抓圖並快取至本地。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalFakeImageFetcher {

  private static final String OPENVERSE_API = "https://api.openverse.org/v1/images/";
  private static final String WIKIMEDIA_API = "https://commons.wikimedia.org/w/api.php";
  private static final String UNSPLASH_SEARCH = "https://api.unsplash.com/search/photos";
  private static final int MIN_IMAGE_BYTES = 12_000;
  private static final int MIN_DIMENSION = 320;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${game.fake-image.unsplash-access-key:}")
  private String unsplashAccessKey;

  @Value("${game.fake-image.enable-openverse:true}")
  private boolean enableOpenverse;

  @Value("${game.fake-image.enable-wikimedia:true}")
  private boolean enableWikimedia;

  @Value("${game.fake-image.enable-unsplash:true}")
  private boolean enableUnsplash;

  @Value("${game.fake-image.enable-lorem-flickr:true}")
  private boolean enableLoremFlickr;

  public Optional<String> fetchAndSave(
      FakeImageRegion region, FakeImageCategory category, Path themeDir) {
    try {
      Files.createDirectories(themeDir);
    } catch (IOException e) {
      log.error("無法建立假圖目錄: {}", themeDir, e);
      return Optional.empty();
    }

    List<String> queries = new ArrayList<>(FakeImageSearchQueryBuilder.buildQueries(region, category));
    Collections.shuffle(queries);

    if (enableOpenverse) {
      for (String query : queries) {
        Optional<String> saved =
            tryDownloadCandidates(searchOpenverse(query), themeDir, "Openverse");
        if (saved.isPresent()) {
          log.info("Openverse 假圖: {} / {} -> {}", region, category, saved.get());
          return saved;
        }
      }
    }

    if (enableWikimedia) {
      for (String query : queries) {
        Optional<String> saved =
            tryDownloadCandidates(searchWikimedia(query), themeDir, "Wikimedia");
        if (saved.isPresent()) {
          log.info("Wikimedia 假圖: {} / {} -> {}", region, category, saved.get());
          return saved;
        }
      }
    }

    if (enableUnsplash && unsplashAccessKey != null && !unsplashAccessKey.isBlank()) {
      for (String query : queries) {
        Optional<String> saved =
            tryDownloadCandidates(searchUnsplash(query), themeDir, "Unsplash");
        if (saved.isPresent()) {
          log.info("Unsplash 假圖: {} / {} -> {}", region, category, saved.get());
          return saved;
        }
      }
    }

    if (enableLoremFlickr) {
      Optional<String> saved = fetchLoremFlickr(region, category, themeDir);
      if (saved.isPresent()) {
        log.info("LoremFlickr 假圖: {} / {} -> {}", region, category, saved.get());
        return saved;
      }
    }

    // Picsum 僅在主題搜尋皆失敗時作為最後備援（隨機圖，與主題無關）
    Optional<String> picsum = fetchPicsum(region, category, themeDir);
    if (picsum.isPresent()) {
      log.warn("Picsum 備援假圖（非主題圖）: {} / {} -> {}", region, category, picsum.get());
      return picsum;
    }

    log.warn("所有圖源皆無可用圖片: {} / {}", region, category);
    return Optional.empty();
  }

  private List<String> searchOpenverse(String query) {
    try {
      URI uri =
          UriComponentsBuilder.fromHttpUrl(OPENVERSE_API)
              .queryParam("q", query)
              .queryParam("page_size", "20")
              .queryParam("license", "cc0,pdm,by,by-sa")
              .build()
              .encode()
              .toUri();

      String body = restTemplate.getForObject(uri, String.class);
      if (body == null || body.isBlank()) {
        return List.of();
      }

      JsonNode results = objectMapper.readTree(body).path("results");
      List<String> urls = new ArrayList<>();
      if (results.isArray()) {
        for (JsonNode item : results) {
          int width = item.path("width").asInt(0);
          int height = item.path("height").asInt(0);
          if (width > 0 && width < MIN_DIMENSION && height > 0 && height < MIN_DIMENSION) {
            continue;
          }
          String url = item.path("url").asText("");
          if (!url.isBlank() && isPhotoUrl(url)) {
            urls.add(url);
          }
        }
      }
      Collections.shuffle(urls);
      return urls;
    } catch (Exception e) {
      log.warn("Openverse 搜尋失敗: query={}", query, e);
      return List.of();
    }
  }

  private List<String> searchWikimedia(String query) {
    try {
      URI uri =
          UriComponentsBuilder.fromHttpUrl(WIKIMEDIA_API)
              .queryParam("action", "query")
              .queryParam("generator", "search")
              .queryParam("gsrsearch", query)
              .queryParam("gsrnamespace", "6")
              .queryParam("gsrlimit", "15")
              .queryParam("prop", "imageinfo")
              .queryParam("iiprop", "url|mime|size")
              .queryParam("iiurlwidth", "1200")
              .queryParam("format", "json")
              .build()
              .encode()
              .toUri();

      String body = restTemplate.getForObject(uri, String.class);
      if (body == null || body.isBlank()) {
        return List.of();
      }

      JsonNode pages = objectMapper.readTree(body).path("query").path("pages");
      List<String> urls = new ArrayList<>();
      Iterator<String> fieldNames = pages.fieldNames();
      while (fieldNames.hasNext()) {
        JsonNode page = pages.get(fieldNames.next());
        JsonNode imageInfo = page.path("imageinfo");
        if (!imageInfo.isArray() || imageInfo.isEmpty()) {
          continue;
        }
        JsonNode info = imageInfo.get(0);
        String mime = info.path("mime").asText("");
        if (mime.contains("svg") || mime.contains("gif")) {
          continue;
        }
        String url = info.path("thumburl").asText("");
        if (url.isBlank()) {
          url = info.path("url").asText("");
        }
        if (!url.isBlank() && isPhotoUrl(url)) {
          urls.add(url);
        }
      }
      Collections.shuffle(urls);
      return urls;
    } catch (Exception e) {
      log.warn("Wikimedia 搜尋失敗: query={}", query, e);
      return List.of();
    }
  }

  private List<String> searchUnsplash(String query) {
    try {
      URI uri =
          UriComponentsBuilder.fromHttpUrl(UNSPLASH_SEARCH)
              .queryParam("query", query)
              .queryParam("per_page", "20")
              .queryParam("orientation", "squarish")
              .build()
              .encode()
              .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Client-ID " + unsplashAccessKey);
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      var response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
      String body = response.getBody();
      if (body == null || body.isBlank()) {
        return List.of();
      }

      JsonNode results = objectMapper.readTree(body).path("results");
      List<String> urls = new ArrayList<>();
      if (results.isArray()) {
        for (JsonNode item : results) {
          String url = item.path("urls").path("regular").asText("");
          if (!url.isBlank()) {
            urls.add(url);
          }
        }
      }
      Collections.shuffle(urls);
      return urls;
    } catch (Exception e) {
      log.warn("Unsplash 搜尋失敗: query={}", query, e);
      return List.of();
    }
  }

  private Optional<String> fetchLoremFlickr(
      FakeImageRegion region, FakeImageCategory category, Path themeDir) {
    String tag1 = FakeImageSearchQueryBuilder.loremFlickrRegionTag(region);
    String tag2 = FakeImageSearchQueryBuilder.loremFlickrCategoryTag(category);
    String url = "https://loremflickr.com/900/1200/" + tag1 + "," + tag2 + "/all";
    return downloadToThemeDir(url, themeDir);
  }

  /** 伺服器端下載 Picsum，玩家透過本地 /api/images/ 讀取，避開手機直連問題 */
  private Optional<String> fetchPicsum(
      FakeImageRegion region, FakeImageCategory category, Path themeDir) {
    int seed = (region.name() + "_" + category.name() + "_" + System.currentTimeMillis()).hashCode();
    String url = "https://picsum.photos/seed/" + Math.abs(seed) + "/900/1200";
    return downloadToThemeDir(url, themeDir);
  }

  private Optional<String> tryDownloadCandidates(
      List<String> urls, Path themeDir, String source) {
    int limit = Math.min(urls.size(), 12);
    for (int i = 0; i < limit; i++) {
      Optional<String> saved = downloadToThemeDir(urls.get(i), themeDir);
      if (saved.isPresent()) {
        return saved;
      }
    }
    if (limit > 0) {
      log.debug("{} 下載 {} 個候選皆失敗", source, limit);
    }
    return Optional.empty();
  }

  private Optional<String> downloadToThemeDir(String imageUrl, Path themeDir) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.USER_AGENT, "BluffMaster/1.0 (party-game)");
      if (imageUrl.contains("wikimedia.org")) {
        headers.set(HttpHeaders.REFERER, "https://commons.wikimedia.org/");
      }
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      byte[] data =
          restTemplate
              .exchange(imageUrl, HttpMethod.GET, entity, byte[].class)
              .getBody();

      if (data == null || data.length < MIN_IMAGE_BYTES) {
        log.debug("圖片太小或為空: {} bytes, url={}", data == null ? 0 : data.length, imageUrl);
        return Optional.empty();
      }
      if (!looksLikeImage(data)) {
        return Optional.empty();
      }

      String fileName =
          "web_"
              + System.currentTimeMillis()
              + "_"
              + ThreadLocalRandom.current().nextInt(10000)
              + ".jpg";
      Path target = themeDir.resolve(fileName);
      Files.write(target, data);
      return Optional.of(fileName);
    } catch (Exception e) {
      log.warn("下載圖片失敗: {} - {}", imageUrl, e.getMessage());
      return Optional.empty();
    }
  }

  private boolean isPhotoUrl(String url) {
    String lower = url.toLowerCase();
    return !lower.endsWith(".svg")
        && !lower.endsWith(".gif")
        && !lower.contains(".svg?")
        && !lower.contains("icon")
        && !lower.contains("logo");
  }

  private boolean looksLikeImage(byte[] data) {
    if (data.length < 4) {
      return false;
    }
    if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
      return true;
    }
    if (data[0] == (byte) 0x89 && data[1] == 0x50) {
      return true;
    }
    return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
  }
}
