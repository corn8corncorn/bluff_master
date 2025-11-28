# Bluff Master 唬爛王

一款多人即時派對遊戲，適合在露營、民宿、聚會時遊玩。

## 專案架構

```
bluff_master/
├── backend/          # Spring Boot 後端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bluffmaster/
│   │   │   │   ├── config/        # 配置類
│   │   │   │   ├── controller/    # REST API 控制器
│   │   │   │   ├── dto/           # 資料傳輸物件
│   │   │   │   ├── model/         # 實體模型
│   │   │   │   ├── repository/    # 資料存取層
│   │   │   │   ├── service/       # 業務邏輯層
│   │   │   │   └── websocket/     # WebSocket 處理
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
├── frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── views/    # 頁面組件
│   │   ├── stores/   # Pinia 狀態管理
│   │   ├── services/ # API 與 WebSocket 服務
│   │   ├── router/   # 路由配置
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## 技術棧

### 後端
- **Spring Boot 3.2.0** - Java 後端框架
- **WebSocket (STOMP)** - 即時通訊
- **MySQL** - 資料庫
- **Redis** - 快取與會話管理
- **GCP Storage** - 圖片儲存
- **Thumbnailator** - 圖片壓縮

### 前端
- **Vue 3** - 前端框架
- **Pinia** - 狀態管理
- **Vue Router** - 路由管理
- **Tailwind CSS** - 樣式框架
- **SockJS + STOMP** - WebSocket 客戶端
- **Axios** - HTTP 客戶端
- **Vite** - 建置工具

## 環境需求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

## 安裝與執行

### 後端設定

1. 建立 MySQL 資料庫：
```sql
CREATE DATABASE bluff_master CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 設定環境變數（或修改 `application.yml`）：
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export GCP_BUCKET_NAME=bluff-master-images
export GCP_PROJECT_ID=your-project-id
export GCP_CREDENTIALS_PATH=/path/to/credentials.json
```

3. 編譯與執行：
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

後端服務將在 `http://localhost:8080` 啟動。

### 前端設定

1. 安裝依賴：
```bash
cd frontend
npm install
```

2. 啟動開發伺服器：
```bash
npm run dev
```

前端應用將在 `http://localhost:3000` 啟動。

## 功能說明

### 核心功能

1. **房間管理**
   - 創建房間（設定人數與遊戲模式）
   - 加入房間（使用 6 位房間代碼）
   - 房主可踢人
   - 遊戲開始後禁止加入

2. **玩家管理**
   - 設定暱稱（開始後鎖定）
   - 上傳圖片（批量上傳，自動壓縮）
   - Ready 狀態管理

3. **遊戲流程**
   - 輪流選主講者
   - 系統抽取 3 真圖 + 1 假圖（快速模式：2 真 + 1 假）
   - 主講者講述圖片
   - 其他玩家投票（10 秒倒數）
   - 自動計分與結算

4. **積分系統**
   - 主講者：最高分 = ceil(玩家數 × 0.5)
   - 投票者：投中 +1，投錯 0，未投票 -1

5. **即時同步**
   - WebSocket 即時推送房間狀態
   - 投票狀態即時更新
   - 斷線重連處理

## API 文件

### 房間相關

- `POST /api/rooms` - 創建房間
- `POST /api/rooms/join` - 加入房間
- `GET /api/rooms/{roomId}` - 獲取房間資訊
- `GET /api/rooms/code/{roomCode}` - 根據代碼獲取房間
- `POST /api/rooms/{roomId}/players/{playerId}/ready` - 玩家準備
- `POST /api/rooms/{roomId}/host/{hostId}/start` - 開始遊戲

### 圖片相關

- `POST /api/images/players/{playerId}/upload` - 上傳圖片
- `DELETE /api/images/players/{playerId}` - 刪除玩家圖片
- `DELETE /api/images/rooms/{roomId}` - 刪除房間所有圖片

### 遊戲相關

- `POST /api/game/rooms/{roomId}/rounds/start` - 開始新回合
- `POST /api/game/rounds/{roundId}/vote` - 投票
- `POST /api/game/rounds/{roundId}/finish` - 結束回合

## WebSocket 事件

### 訂閱頻道

- `/topic/room/{roomId}` - 房間狀態更新
- `/topic/room/{roomId}/round` - 回合開始
- `/topic/room/{roomId}/vote` - 投票狀態
- `/topic/room/{roomId}/round-result` - 回合結果
- `/topic/room/{roomId}/player-disconnect` - 玩家斷線

### 發送訊息

- `/app/room/update` - 請求房間更新
- `/app/game/start-round` - 開始回合
- `/app/game/vote` - 發送投票
- `/app/game/finish-round` - 結束回合

## 部署

### 後端部署

1. 打包應用：
```bash
cd backend
mvn clean package
```

2. 執行 JAR：
```bash
java -jar target/bluff-master-backend-1.0.0.jar
```

### 前端部署

1. 建置生產版本：
```bash
cd frontend
npm run build
```

2. 部署 `dist` 目錄到靜態檔案伺服器（如 Nginx）

## Cloudflare Tunnel 遠端 DEMO

如果要讓其他人透過網路訪問您的本地開發伺服器進行 DEMO，可以使用 Cloudflare Tunnel。

### 前置需求

1. **安裝 Cloudflare Tunnel**：
   ```bash
   # Windows (使用 winget)
   winget install --id Cloudflare.cloudflared
   
   # macOS
   brew install cloudflare/cloudflare/cloudflared
   
   # Linux
   # 參考官方文檔：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/
   ```

### 使用方式

#### 方法 1: 使用完整啟動腳本（推薦）

```bash
# 自動啟動後端、前端和 Cloudflare Tunnel
start-demo-full.bat
```

#### 方法 2: 手動啟動

1. **確保後端和前端已啟動**：
   - 後端：`http://localhost:8080`
   - 前端：`http://localhost:3000`

2. **啟動 Cloudflare Tunnel**：
   ```bash
   # Windows
   start-demo.bat
   
   # 或 PowerShell
   .\start-demo.ps1
   ```

3. **分享生成的 URL**：
   Cloudflare Tunnel 會生成類似以下的 URL：
   ```
   https://xxxx-xxxx-xxxx.trycloudflare.com
   ```
   將此 URL 分享給其他人即可訪問您的本地 DEMO。

### 注意事項

- Cloudflare Tunnel URL 在每次啟動時會變化
- 連接會在使用者關閉終端或中斷連接時自動關閉
- 確保後端服務已啟動，否則前端無法正常工作
- WebSocket 連接會通過 Cloudflare Tunnel 自動處理

## 注意事項

1. **GCP Storage 設定**：需要建立 GCS Bucket 並設定適當的權限
2. **圖片清理**：系統會自動清理超過 3 小時的圖片
3. **斷線處理**：主講者斷線會自動結束當前回合
4. **投票超時**：10 秒內未投票會自動扣 1 分
5. **Cloudflare Tunnel**：僅用於 DEMO，生產環境請使用正式部署方案

## 授權

MIT License

