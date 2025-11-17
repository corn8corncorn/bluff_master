# 本地端啟動指南

本指南將幫助您在本地端啟動 Bluff Master 唬爛王專案。

## 前置需求

### 1. 安裝必要軟體

- **Java 17+** - [下載連結](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [下載連結](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [下載連結](https://nodejs.org/)
- **MySQL 8.0+** - [下載連結](https://dev.mysql.com/downloads/mysql/)
- **Redis 6.0+** - [下載連結](https://redis.io/download)

### 2. 驗證安裝

```bash
# 檢查 Java 版本
java -version

# 檢查 Maven 版本
mvn -version

# 檢查 Node.js 版本
node -v
npm -v

# 檢查 MySQL 版本
mysql --version

# 檢查 Redis 版本
redis-cli --version
```

## 步驟 1: 設定資料庫

### 1.1 啟動 MySQL

```bash
# Windows (以管理員身份執行)
net start MySQL80

# macOS (使用 Homebrew)
brew services start mysql

# Linux
sudo systemctl start mysql
```

### 1.2 建立資料庫

```bash
# 登入 MySQL
mysql -u root -p

# 執行初始化腳本
source backend/init-db.sql

# 或手動執行
CREATE DATABASE IF NOT EXISTS bluff_master CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.3 修改資料庫配置（如需要）

編輯 `backend/src/main/resources/application.yml` 或使用環境變數：

```bash
# Windows PowerShell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"

# macOS/Linux
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

## 步驟 2: 啟動 Redis

```bash
# Windows (需要下載 Redis for Windows)
redis-server

# macOS (使用 Homebrew)
brew services start redis

# Linux
sudo systemctl start redis
```

驗證 Redis 是否運行：

```bash
redis-cli ping
# 應該返回: PONG
```

## 步驟 3: 啟動後端服務

### 3.1 進入後端目錄

```bash
cd backend
```

### 3.2 編譯專案

```bash
mvn clean install
```

### 3.3 啟動 Spring Boot 應用

```bash
# 使用 Maven
mvn spring-boot:run

# 或使用開發配置
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

後端服務將在 `http://localhost:8080` 啟動。

### 3.4 驗證後端是否正常運行

開啟瀏覽器訪問：`http://localhost:8080/api`

或使用 curl：

```bash
curl http://localhost:8080/api
```

## 步驟 4: 啟動前端服務

### 4.1 開啟新的終端視窗

保持後端服務運行，開啟新的終端視窗。

### 4.2 進入前端目錄

```bash
cd frontend
```

### 4.3 安裝依賴

```bash
npm install
```

### 4.4 啟動開發伺服器

```bash
npm run dev
```

前端應用將在 `http://localhost:3000` 啟動。

### 4.5 驗證前端是否正常運行

開啟瀏覽器訪問：`http://localhost:3000`

## 步驟 5: 測試應用

1. 開啟瀏覽器訪問 `http://localhost:3000`
2. 點擊「創建房間」
3. 輸入暱稱、選擇遊戲模式和人數
4. 上傳圖片
5. 點擊「準備」
6. 開始遊戲

## 常見問題

### 問題 1: MySQL 連接失敗

**錯誤訊息**: `Communications link failure`

**解決方法**:
- 確認 MySQL 服務正在運行
- 檢查 `application.yml` 中的資料庫配置
- 確認資料庫用戶名和密碼正確
- 確認 MySQL 允許本地連接

### 問題 2: Redis 連接失敗

**錯誤訊息**: `Unable to connect to Redis`

**解決方法**:
- 確認 Redis 服務正在運行
- 檢查 Redis 端口是否為 6379
- 如果 Redis 有密碼，在 `application.yml` 中設定

### 問題 3: 端口已被占用

**錯誤訊息**: `Port 8080 is already in use`

**解決方法**:
- 修改 `application.yml` 中的 `server.port`
- 或關閉占用端口的程序

```bash
# Windows 查看端口占用
netstat -ano | findstr :8080

# macOS/Linux 查看端口占用
lsof -i :8080
```

### 問題 4: 前端無法連接後端

**解決方法**:
- 確認後端服務正在運行
- 檢查 `frontend/vite.config.js` 中的代理配置
- 確認後端端口為 8080

### 問題 5: GCP Storage 錯誤（本地開發可忽略）

如果看到 GCP Storage 相關錯誤，可以暫時忽略（圖片上傳功能會受影響）。

要啟用圖片上傳功能，需要：
1. 建立 GCP 專案
2. 啟用 Cloud Storage API
3. 建立 Storage Bucket
4. 下載服務帳戶憑證
5. 設定環境變數：
   ```bash
   export GCP_PROJECT_ID=your-project-id
   export GCP_BUCKET_NAME=bluff-master-images
   export GCP_CREDENTIALS_PATH=/path/to/credentials.json
   ```

## 快速啟動腳本

### Windows (start.bat)

```batch
@echo off
echo Starting Bluff Master...

echo Starting MySQL...
net start MySQL80

echo Starting Redis...
start "Redis" redis-server

echo Starting Backend...
cd backend
start "Backend" cmd /k "mvn spring-boot:run"

timeout /t 10

echo Starting Frontend...
cd ../frontend
start "Frontend" cmd /k "npm run dev"

echo All services started!
pause
```

### macOS/Linux (start.sh)

```bash
#!/bin/bash

echo "Starting Bluff Master..."

# 啟動 MySQL
echo "Starting MySQL..."
brew services start mysql 2>/dev/null || sudo systemctl start mysql

# 啟動 Redis
echo "Starting Redis..."
brew services start redis 2>/dev/null || sudo systemctl start redis

# 啟動後端
echo "Starting Backend..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!

# 等待後端啟動
sleep 10

# 啟動前端
echo "Starting Frontend..."
cd ../frontend
npm run dev &
FRONTEND_PID=$!

echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "All services started!"
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:3000"

# 等待用戶中斷
wait
```

## 停止服務

### 停止後端
在後端終端視窗按 `Ctrl + C`

### 停止前端
在前端終端視窗按 `Ctrl + C`

### 停止 MySQL
```bash
# Windows
net stop MySQL80

# macOS
brew services stop mysql

# Linux
sudo systemctl stop mysql
```

### 停止 Redis
```bash
# Windows
redis-cli shutdown

# macOS
brew services stop redis

# Linux
sudo systemctl stop redis
```

## 開發建議

1. **使用 IDE**: 推薦使用 IntelliJ IDEA (後端) 和 VS Code (前端)
2. **熱重載**: 前端使用 Vite 自動熱重載，後端需要重啟
3. **日誌查看**: 後端日誌會顯示在終端，前端日誌在瀏覽器控制台
4. **資料庫工具**: 推薦使用 MySQL Workbench 或 DBeaver 查看資料庫

## 下一步

- 閱讀 [README.md](README.md) 了解完整功能
- 查看 API 文件了解後端接口
- 開始開發新功能！

