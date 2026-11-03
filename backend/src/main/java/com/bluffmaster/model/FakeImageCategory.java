package com.bluffmaster.model;

import lombok.Getter;

@Getter
public enum FakeImageCategory {
    FOOD("美食"),
    SCENERY("風景"),
    CULTURE("人文"),
    ARCHITECTURE("建築"),
    ART("藝術作品"),
    DAILY_LIFE("日常生活"),
    SHOPPING("購物商品");

    private final String label;

    FakeImageCategory(String label) {
        this.label = label;
    }
}
