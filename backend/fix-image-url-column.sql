-- 修復 image_url 欄位長度問題
-- 將 VARCHAR 改為 TEXT 以支持 Base64 編碼的圖片數據

-- 修改 player_images 表
ALTER TABLE player_images MODIFY COLUMN image_url TEXT;

-- 修改 round_images 表
ALTER TABLE round_images MODIFY COLUMN image_url TEXT;

-- 修改 round_votes 表
ALTER TABLE round_votes MODIFY COLUMN voted_image_url TEXT;

-- 修改 game_rounds 表的 fakeImageUrl 欄位
ALTER TABLE game_rounds MODIFY COLUMN fake_image_url TEXT;

