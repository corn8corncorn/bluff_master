package com.bluffmaster.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 修正舊版資料庫 schema：開局時 fake_image_url 尚未選定，須允許 NULL。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        patchNullableColumn("game_rounds", "fake_image_url", "TEXT");
        patchNullableColumn("game_rounds", "fake_image_region", "VARCHAR(50)");
        patchNullableColumn("game_rounds", "fake_image_category", "VARCHAR(50)");
        addColumnIfMissing("game_rounds", "fake_image_region", "VARCHAR(50) NULL");
        addColumnIfMissing("game_rounds", "fake_image_category", "VARCHAR(50) NULL");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("Schema patch: 新增欄位 {}.{}", table, column);
        } catch (Exception e) {
            log.debug("Schema patch add column skipped for {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void patchNullableColumn(String table, String column, String sqlType) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + sqlType + " NULL");
            log.info("Schema patch: {}.{} 已改為允許 NULL", table, column);
        } catch (Exception e) {
            log.debug("Schema patch skipped for {}.{}: {}", table, column, e.getMessage());
        }
    }
}
