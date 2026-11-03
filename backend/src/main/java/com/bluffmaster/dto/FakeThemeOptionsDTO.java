package com.bluffmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FakeThemeOptionsDTO {
    private List<ThemeOption> regions;
    private List<ThemeOption> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThemeOption {
        private String id;
        private String label;
    }
}
