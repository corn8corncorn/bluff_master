# Bluff Master 唬爛王

多人即時派對遊戲：輪流當主講者，從真圖混入一張假圖，其他人投票找出唬爛圖。適合露營、民宿、聚會同樂。

## 架構

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.2（Java 17+）、REST API、WebSocket（STOMP + SockJS） |
| 前端 | Vue 3、Vite、Pinia、Vue Router、Tailwind CSS、Axios |
| 資料 | MySQL 8（本機開發常用 `dev` profile） |
| 即時 | 房間狀態、回合、投票、結果皆經 WebSocket 推送；Vite 將 `/api`（含 WS）代理到後端 |

**預設不需要 Redis**（程式未使用；`application.yml` 已排除 Redis auto-config，B5 已修）。

## 目錄結構

```
bluff_master/
├── backend/                 # Spring Boot
│   ├── src/main/java/com/bluffmaster/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── websocket/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   ├── start.bat / start.sh # 後端單獨啟動（JAR 優先，否則 Maven）
│   └── pom.xml
├── frontend/                # Vue 3 + Vite
│   ├── src/
│   │   ├── views/
│   │   ├── stores/
│   │   ├── services/
│   │   └── router/
│   ├── package.json         # 以 cross-env 設定 NODE_ENV
│   └── vite.config.js
├── start-demo.bat           # Windows 一鍵：後端 + 前端 + Cloudflare Tunnel
├── start-demo-full.bat      # Windows 完整 DEMO（同邏輯）
├── start.sh                 # Linux／macOS 一鍵（對齊 full）
├── package.json             # 根目錄腳本 + cross-env
├── BUGFIX_LOG.md
├── QUICK_START.md
├── DEMO_GUIDE.md
└── README.md
```

## 環境需求

- **Java 17+**
- **Node.js 18+**
- **MySQL 8.0+**（需先建立資料庫）
- Maven 3.6+（若尚未編譯 JAR；有 JAR 時可略過）
- Cloudflare Tunnel（`cloudflared`，僅一鍵 DEMO／對外分享時需要）
- **不需要 Redis**

前端與根目錄 npm scripts 已用 **cross-env** 處理 `NODE_ENV`（跨 Windows／Linux）。

建立資料庫：

```sql
CREATE DATABASE bluff_master CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

本機後端預設使用 `dev` profile（`application-dev.yml`）。MySQL 帳密可用環境變數 `DB_USERNAME`／`DB_PASSWORD` 覆寫（未設定時預設皆為 `root`）。

## 啟動方式

預設埠：**前端** `http://localhost:3000`，**後端** `http://localhost:8080`（context-path：`/api`）。

### 1. Windows 一鍵（後端 + 前端 + Tunnel）

雙擊或在專案根目錄執行：

```bat
start-demo.bat
```

會自動：

1. 啟動後端（優先 `backend/target/bluff-master-backend-1.0.0.jar`，否則 `mvn spring-boot:run`，並帶 `dev` profile）
2. 啟動前端 `npm run dev`
3. 啟動 Cloudflare Tunnel 暴露前端（分享產生的 `https://….trycloudflare.com`）

需已安裝 `cloudflared`、Node.js、Java（以及 JAR 不存在時的 Maven）。

### 2. Linux／macOS 一鍵

```bash
chmod +x start.sh
./start.sh
```

行為對齊 Windows 完整 DEMO：後端（JAR 或 Maven）+ 前端 + Cloudflare Tunnel。路徑一律使用 `/`。

### 3. 手動分開啟動

**視窗 A — 後端：**

```bash
cd backend
# 建議：有 JAR 時
java -Dspring.profiles.active=dev -jar target/bluff-master-backend-1.0.0.jar
# 或：
# ./start.sh          # Linux（JAR 優先）
# start.bat           # Windows
# mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**視窗 B — 前端：**

```bash
cd frontend
npm install
npm run dev
```

或在根目錄：`npm run dev:frontend`（經 cross-env 設 `NODE_ENV=development`）。

## 常見問題

### 連接埠被佔用（3000 / 8080）

結束佔用該埠的行程，或暫時改 `frontend/vite.config.js` 的 `server.port`、後端 `server.port`。一鍵腳本預設仍指向 3000／8080。

### 重整後無法同名加入（重連機制，B12）

Session 遺失時，同名 `join` 會被擋。房間頁可選擇**重連原座位**（`POST /rooms/reconnect`，以 `playerId`／暱稱認領）；瀏覽器也會以 localStorage 輔助記住座位。

### 投票逾時房主不在也卡住？（自動結算，B10）

投票逾時後伺服器會自動 reveal，約 **4 秒**後自動 `finishRound` 計分並廣播，不依賴房主在線。房主若提前手動結束會取消自動排程，避免重複計分。

### 下一輪誰可以開？（B8）

「進行下一輪」可由**當前回合主講者或房主**開啟，不必一定是房主。

### 選假圖失敗沒反應？（B13）

選題／抓圖失敗時前端會以 **toast**（及畫面上錯誤訊息）提示，可重試；不會靜默失敗。

### 其他

- JAR 啟動請使用：`java -Dspring.profiles.active=dev -jar …`（`-D` 必須在 `-jar` 之前）
- WebSocket 連線失敗時前端會提示「連線失敗，請重整」
- 詳細修訂紀錄見 [BUGFIX_LOG.md](BUGFIX_LOG.md)（B1–B13 已完成）

## 文件索引

| 文件 | 說明 |
|------|------|
| [QUICK_START.md](QUICK_START.md) | 3 分鐘快速開始 |
| [啟動說明.md](啟動說明.md) | 本地啟動完整說明（正本） |
| [啟動步驟.md](啟動步驟.md) | 已合併，轉導至上方兩份 |
| [DEMO_GUIDE.md](DEMO_GUIDE.md) | Cloudflare Tunnel DEMO 說明 |
| [如何分享給其他人.md](如何分享給其他人.md) | 把 Tunnel 網址分給朋友 |
| [BUGFIX_LOG.md](BUGFIX_LOG.md) | Bug 修復與審查紀錄 |
| [START.md](START.md) | 較完整的啟動／故障排除 |

## 測試沙盒 (Docker)

本機不想裝 MySQL 時，可用 Docker 起資料庫再測前後端：

```bash
docker-compose up -d
```

會啟動 `mysql:8.0`（埠 `3306`），資料庫 `bluff_master`，帳密 `root`／`root`，資料持久化在 volume `bluff_master_mysql_data`。

一鍵（起 MySQL → 等 10 秒 → 後端 JAR／Maven → 前端）：

- Windows：`test-sandbox.bat`
- Linux／macOS：`chmod +x test-sandbox.sh && ./test-sandbox.sh`

測完可開：**http://localhost:3000**（後端 `http://localhost:8080/api`）。停止沙盒腳本不會刪 MySQL 容器；要停庫可執行 `docker-compose down`。

## 授權

MIT License
