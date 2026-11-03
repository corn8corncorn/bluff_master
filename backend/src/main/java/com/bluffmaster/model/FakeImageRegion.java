package com.bluffmaster.model;

import lombok.Getter;

@Getter
public enum FakeImageRegion {
    TAIWAN("台灣"),
    JAPAN("日本"),
    KOREA("韓國"),
    CHINA("中國"),
    HONG_KONG("香港"),
    MACAU("澳門"),
    THAILAND("泰國"),
    VIETNAM("越南"),
    SINGAPORE("新加坡"),
    MALAYSIA("馬來西亞"),
    INDONESIA("印尼"),
    PHILIPPINES("菲律賓"),
    USA("美國"),
    CANADA("加拿大"),
    UK("英國"),
    FRANCE("法國"),
    ITALY("義大利"),
    GERMANY("德國"),
    SPAIN("西班牙"),
    SWITZERLAND("瑞士"),
    AUSTRALIA("澳洲"),
    NEW_ZEALAND("紐西蘭"),
    OTHER("其他地區"),
    /** 舊版相容，與 OTHER 共用圖庫；不可再由前端選取 */
    FOREIGN("其他國外");

    private final String label;

    FakeImageRegion(String label) {
        this.label = label;
    }

    /** 是否允許玩家新選此地區（FOREIGN 僅保留舊資料相容） */
    public boolean isSelectable() {
        return this != FOREIGN;
    }
}
