package com.bluffmaster.model;

import lombok.Getter;

@Getter
public enum FakeImageTheme {
    TAIWAN_SCENERY("台灣風景", "taiwan-scenery"),
    FOREIGN_SCENERY("外國風景", "foreign-scenery"),
    TAIWAN_FOOD("台灣美食", "taiwan-food"),
    FOREIGN_FOOD("外國美食", "foreign-food"),
    TAIWAN_DAILY("台灣日常", "taiwan-daily"),
    FOREIGN_DAILY("外國日常", "foreign-daily");

    private final String label;
    private final String folderName;

    FakeImageTheme(String label, String folderName) {
        this.label = label;
        this.folderName = folderName;
    }

    public static FakeImageTheme fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException("請選擇假圖主題");
        }
        try {
            return FakeImageTheme.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("無效的假圖主題: " + value);
        }
    }
}
