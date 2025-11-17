-- 建立資料庫
CREATE DATABASE IF NOT EXISTS bluff_master CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用資料庫
USE bluff_master;

-- 注意：JPA 會自動建立表格，此檔案僅供參考
-- 如果需要手動建立，可以使用以下 SQL：

-- CREATE TABLE IF NOT EXISTS rooms (
--     id VARCHAR(255) PRIMARY KEY,
--     room_code VARCHAR(6) UNIQUE NOT NULL,
--     max_players INT NOT NULL,
--     game_mode VARCHAR(20) NOT NULL,
--     status VARCHAR(20) NOT NULL,
--     current_round INT NOT NULL DEFAULT 0,
--     total_rounds INT NOT NULL,
--     created_at DATETIME NOT NULL,
--     updated_at DATETIME NOT NULL,
--     started_at DATETIME
-- );

-- CREATE TABLE IF NOT EXISTS players (
--     id VARCHAR(255) PRIMARY KEY,
--     nickname VARCHAR(100) NOT NULL,
--     room_id VARCHAR(255) NOT NULL,
--     is_host BOOLEAN NOT NULL DEFAULT FALSE,
--     is_ready BOOLEAN NOT NULL DEFAULT FALSE,
--     score INT NOT NULL DEFAULT 0,
--     is_online BOOLEAN NOT NULL DEFAULT TRUE,
--     created_at DATETIME NOT NULL,
--     updated_at DATETIME NOT NULL,
--     FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
-- );

-- CREATE TABLE IF NOT EXISTS player_images (
--     player_id VARCHAR(255) NOT NULL,
--     image_url VARCHAR(500) NOT NULL,
--     PRIMARY KEY (player_id, image_url),
--     FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
-- );

-- CREATE TABLE IF NOT EXISTS game_rounds (
--     id VARCHAR(255) PRIMARY KEY,
--     room_id VARCHAR(255) NOT NULL,
--     round_number INT NOT NULL,
--     speaker_id VARCHAR(255) NOT NULL,
--     fake_image_url VARCHAR(500) NOT NULL,
--     is_finished BOOLEAN NOT NULL DEFAULT FALSE,
--     created_at DATETIME NOT NULL,
--     finished_at DATETIME,
--     FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
--     FOREIGN KEY (speaker_id) REFERENCES players(id)
-- );

-- CREATE TABLE IF NOT EXISTS round_images (
--     round_id VARCHAR(255) NOT NULL,
--     image_url VARCHAR(500) NOT NULL,
--     PRIMARY KEY (round_id, image_url),
--     FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE
-- );

-- CREATE TABLE IF NOT EXISTS round_votes (
--     round_id VARCHAR(255) NOT NULL,
--     player_id VARCHAR(255) NOT NULL,
--     voted_image_url VARCHAR(500) NOT NULL,
--     PRIMARY KEY (round_id, player_id),
--     FOREIGN KEY (round_id) REFERENCES game_rounds(id) ON DELETE CASCADE
-- );

