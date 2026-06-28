package com.bluffmaster.service;

import com.bluffmaster.model.FakeImageCategory;
import com.bluffmaster.model.FakeImageRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

final class FakeImageSearchQueryBuilder {

  private static final Map<FakeImageRegion, String> REGION_EN = Map.ofEntries(
      Map.entry(FakeImageRegion.TAIWAN, "Taiwan"),
      Map.entry(FakeImageRegion.JAPAN, "Japan"),
      Map.entry(FakeImageRegion.KOREA, "South Korea"),
      Map.entry(FakeImageRegion.CHINA, "China"),
      Map.entry(FakeImageRegion.HONG_KONG, "Hong Kong"),
      Map.entry(FakeImageRegion.MACAU, "Macau"),
      Map.entry(FakeImageRegion.THAILAND, "Thailand"),
      Map.entry(FakeImageRegion.VIETNAM, "Vietnam"),
      Map.entry(FakeImageRegion.SINGAPORE, "Singapore"),
      Map.entry(FakeImageRegion.MALAYSIA, "Malaysia"),
      Map.entry(FakeImageRegion.INDONESIA, "Indonesia"),
      Map.entry(FakeImageRegion.PHILIPPINES, "Philippines"),
      Map.entry(FakeImageRegion.USA, "United States"),
      Map.entry(FakeImageRegion.CANADA, "Canada"),
      Map.entry(FakeImageRegion.UK, "United Kingdom"),
      Map.entry(FakeImageRegion.FRANCE, "France"),
      Map.entry(FakeImageRegion.ITALY, "Italy"),
      Map.entry(FakeImageRegion.GERMANY, "Germany"),
      Map.entry(FakeImageRegion.SPAIN, "Spain"),
      Map.entry(FakeImageRegion.SWITZERLAND, "Switzerland"),
      Map.entry(FakeImageRegion.AUSTRALIA, "Australia"),
      Map.entry(FakeImageRegion.NEW_ZEALAND, "New Zealand"),
      Map.entry(FakeImageRegion.OTHER, "travel destination"),
      Map.entry(FakeImageRegion.FOREIGN, "international travel"));

  private static final Map<FakeImageCategory, String[]> CATEGORY_TERMS =
      Map.of(
          FakeImageCategory.FOOD,
          new String[] {"food", "cuisine", "restaurant dish", "street food", "local specialty"},
          FakeImageCategory.SCENERY,
          new String[] {"landscape", "scenery", "nature", "mountain view", "coastline"},
          FakeImageCategory.CULTURE,
          new String[] {"culture", "tradition", "festival", "temple", "heritage"},
          FakeImageCategory.ARCHITECTURE,
          new String[] {"architecture", "building", "landmark", "skyline", "historic"},
          FakeImageCategory.ART,
          new String[] {"art", "museum", "gallery", "sculpture", "exhibition"},
          FakeImageCategory.DAILY_LIFE,
          new String[] {"street life", "daily life", "market", "city life", "commute"},
          FakeImageCategory.SHOPPING,
          new String[] {"shopping", "market stall", "bazaar", "store", "souvenir"});

  /** 地區＋美食的精準搜尋詞（優先使用） */
  private static final Map<FakeImageRegion, String[]> REGION_FOOD_QUERIES =
      Map.ofEntries(
          Map.entry(
              FakeImageRegion.TAIWAN,
              new String[] {
                "Taiwanese food",
                "Taiwan night market food",
                "Taiwan beef noodle",
                "Taiwan bubble tea",
                "Taiwan street food"
              }),
          Map.entry(
              FakeImageRegion.JAPAN,
              new String[] {
                "Japanese food",
                "Japan sushi",
                "Japan ramen",
                "Tokyo street food",
                "Japanese cuisine"
              }),
          Map.entry(
              FakeImageRegion.KOREA,
              new String[] {
                "Korean food",
                "Korea barbecue",
                "Korean street food",
                "bibimbap",
                "Korean cuisine"
              }),
          Map.entry(
              FakeImageRegion.CHINA,
              new String[] {
                "Chinese food",
                "China cuisine",
                "dim sum",
                "Chinese street food",
                "Sichuan food"
              }),
          Map.entry(
              FakeImageRegion.HONG_KONG,
              new String[] {
                "Hong Kong food",
                "Hong Kong dim sum",
                "Hong Kong street food",
                "Cantonese cuisine"
              }),
          Map.entry(
              FakeImageRegion.THAILAND,
              new String[] {
                "Thai food",
                "Thailand street food",
                "pad thai",
                "Thai cuisine"
              }),
          Map.entry(
              FakeImageRegion.VIETNAM,
              new String[] {
                "Vietnamese food",
                "pho Vietnam",
                "Vietnam street food",
                "banh mi"
              }),
          Map.entry(
              FakeImageRegion.SINGAPORE,
              new String[] {
                "Singapore food",
                "Singapore hawker centre",
                "laksa",
                "chili crab Singapore"
              }),
          Map.entry(
              FakeImageRegion.ITALY,
              new String[] {
                "Italian food",
                "pizza Italy",
                "pasta Italy",
                "Italian cuisine"
              }),
          Map.entry(
              FakeImageRegion.FRANCE,
              new String[] {
                "French food",
                "French cuisine",
                "Paris bakery",
                "French restaurant"
              }));

  private FakeImageSearchQueryBuilder() {}

  static List<String> buildQueries(FakeImageRegion region, FakeImageCategory category) {
    String regionEn = REGION_EN.getOrDefault(region, "travel");
    List<String> queries = new ArrayList<>();

    if (category == FakeImageCategory.FOOD) {
      String[] foodQueries = REGION_FOOD_QUERIES.get(region);
      if (foodQueries != null) {
        Collections.addAll(queries, foodQueries);
      }
    }

    String[] terms = CATEGORY_TERMS.get(category);
    if (terms == null || terms.length == 0) {
      queries.add(regionEn + " photo");
    } else {
      ThreadLocalRandom rnd = ThreadLocalRandom.current();
      for (int i = 0; i < 4; i++) {
        String term = terms[rnd.nextInt(terms.length)];
        queries.add(regionEn + " " + term);
      }
      queries.add(regionEn + " " + category.name().toLowerCase().replace('_', ' '));
    }

    return queries.stream().distinct().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  static String pickOneQuery(FakeImageRegion region, FakeImageCategory category) {
    List<String> queries = buildQueries(region, category);
    return queries.get(ThreadLocalRandom.current().nextInt(queries.size()));
  }

  /** LoremFlickr 標籤（單字、無空白） */
  static String loremFlickrRegionTag(FakeImageRegion region) {
    return switch (region) {
      case KOREA -> "korea";
      case USA -> "usa";
      case UK -> "london";
      case NEW_ZEALAND -> "newzealand";
      case HONG_KONG -> "hongkong";
      case OTHER, FOREIGN -> "travel";
      default -> {
        String en = REGION_EN.getOrDefault(region, "travel").toLowerCase();
        yield en.split("\\s+")[0];
      }
    };
  }

  static String loremFlickrCategoryTag(FakeImageCategory category) {
    return switch (category) {
      case FOOD -> "food";
      case SCENERY -> "landscape";
      case CULTURE -> "culture";
      case ARCHITECTURE -> "architecture";
      case ART -> "art";
      case DAILY_LIFE -> "street";
      case SHOPPING -> "shopping";
    };
  }
}
