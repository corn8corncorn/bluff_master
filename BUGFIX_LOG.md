# Bluff Master BUGFIX Log

工作原則：每次只改一小範圍；不確定項只列清單不直接修；路徑用 `/`；環境變數用 `cross-env`。

補充審查來源：[Explore bugs](1c241b9b-f3f9-4822-86d4-5942bcbe3372)（2026-11-03）

---

## 審查清單（2026-11-03）

### Confirmed（已確認，可依序小範圍修復）

| ID | 嚴重度 | 位置 | 問題 | 建議修法（單點） |
|----|--------|------|------|------------------|
| B1 | High | `frontend/vite.config.js` | Proxy `/api` 未開 `ws: true` | ✅ 已修 |
| B2 | High | `backend/start.sh` | 無 JAR 後備；與 bat 不一致 | ✅ 已修 |
| B3 | Medium | 啟動 bat 腳本 | JAR 路徑使用 `\` | ✅ 已修 |
| B4 | High | 專案根目錄 | 無 Linux 一鍵、未用 `cross-env`；`start-demo.bat` 不啟動後端 | ✅ 已修 |
| B5 | High | `application.yml` | 預設啟用 Redis，程式未用 | ✅ 已修 |
| B6 | Medium | `websocket.js` / `game.js` | `connect()` 失敗回傳 `null`，後續靜默失敗 | ✅ 已修 |
| B7 | Critical | `start.bat` / `start-jar.bat` / `start-demo-full.bat` | `-D` 放在 `-jar` 之後，dev profile 不生效 | ✅ 已修：`java -Dspring.profiles.active=dev -jar ...` |
| B8 | Critical | `GameService.startRound` vs `Game.vue` | UI 讓主講者開下一輪，後端 `requireHost` → 非房主主講者失敗 | ✅ 已修 |
| B9 | High | `GameService` 斷線處理 | 主講者斷線呼叫 `finishRound`，若尚無假圖會丟錯，回合卡住 | ✅ 已修 |
| B10 | High | `VotingTimeoutScheduler` + `finishRound` | 逾時只 reveal；計分需房主 `finishRound`，房主離線會卡在 REVEALING | ✅ 已修 |
| B11 | High | `votingEndsAt` LocalDateTime | 無時區；跨區 DEMO 倒數錯位 | ✅ 已修 |
| B12 | Medium | `Room.vue` 重新加入 | `join` 拒同名；session 遺失無法重連原座位 | ✅ 已修 |
| B13 | Medium | `WebSocketController.selectFakeTheme` | catch 只 log，WS 備援無錯誤回饋 | 錯誤頻道或回傳失敗 |

### Uncertain（不確定，先不修）

| ID | 嚴重度 | 位置 | 疑點 | 待驗證 |
|----|--------|------|------|--------|
| U1 | Medium | `game.js` upload `Content-Type: multipart/form-data` | 可能缺 boundary；Axios 1.x 多半會覆寫 | 實機上傳是否偶發失敗 |
| U2 | Low | `joinRoom` 用 nickname 找玩家 | 後端已擋同名時風險低 | 回應是否應帶 playerId |
| U3 | Low | `/round` vs `/round-result` | 語意分裂但兩邊都能更新 UI | 是否刻意 |
| U4 | Low | REST `next-phase` 等不廣播 WS | 目前前端走 WS；純 REST 客戶會不同步 | 是否要統一廣播 |
| U5 | Low | `application-dev.yml` 硬編碼密碼 | 不利跨機；屬設定/安全 | 改 `${DB_PASSWORD:...}` |

---

## 修復紀錄

### 2026-11-03 — B1 Vite proxy WebSocket

- **改哪裡**：`frontend/vite.config.js` 的 `server.proxy`
- **為什麼**：`/api` 先匹配且無 `ws: true`，SockJS/STOMP 升級易失敗
- **怎麼改**：單一 `/api` proxy + `ws: true`
- **狀態**：已完成

### 2026-11-03 — B3 啟動腳本路徑改用 /

- **改哪裡**：`backend/start.bat`、`backend/start-jar.bat`、`start-demo-full.bat`
- **為什麼**：路徑規範一律 `/`
- **怎麼改**：`target\...` → `target/...`
- **狀態**：已完成

### 2026-11-03 — B7 JVM `-D` 必須在 `-jar` 之前

- **改哪裡**：同上三支 bat + `backend/README_啟動說明.md`
- **為什麼**：`java -jar -Dspring.profiles.active=dev ...` 會把 `-D` 當成程式參數，dev profile 不載入（錯密碼／誤連 Redis）
- **怎麼改**：`java -Dspring.profiles.active=dev -jar target/bluff-master-backend-1.0.0.jar`
- **來源**：[Explore bugs](1c241b9b-f3f9-4822-86d4-5942bcbe3372)
- **狀態**：已完成

### 2026-11-03 — B2 `start.sh` 對齊 `start.bat`

- **改哪裡**：`backend/start.sh`
- **為什麼**：Linux 腳本先前只走 Maven，無 JAR 時無法啟動，且與 Windows bat 行為不一致
- **怎麼改**：有 `target/bluff-master-backend-1.0.0.jar` 則 `java -Dspring.profiles.active=dev -jar ...`；否則檢查 Maven 後 `mvn spring-boot:run -Dspring-boot.run.profiles=dev`；路徑用 `/`
- **狀態**：已完成

### 2026-11-03 — B4 根目錄一鍵啟動 + cross-env

- **改哪裡**：`start-demo.bat`、根目錄 `start.sh`、根目錄 `package.json`、`frontend/package.json`（及 lockfile）
- **為什麼**：`start-demo.bat` 先前不啟動後端；Linux 無對應一鍵腳本；環境變數需跨平台 `cross-env`
- **怎麼改**：
  1. `start-demo.bat` 對齊 `start-demo-full.bat`：自動啟動後端（JAR 或 Maven）再前端與 Tunnel
  2. 新增根目錄 `start.sh`（後端 + 前端 + Tunnel，路徑 `/`；註解說明 `chmod +x start.sh`）
  3. 新增根目錄 `package.json`；前後端 scripts 以 `cross-env` 設定 `NODE_ENV`（原先無 `set NODE_ENV=`）
- **狀態**：已完成

### 2026-11-03 — B5 預設 profile 排除 Redis auto-config

- **改哪裡**：`backend/src/main/resources/application.yml`
- **為什麼**：預設會啟用 Redis 自動設定，但程式未使用 Redis，未安裝 Redis 時啟動易失敗
- **怎麼改**：參考 `application-dev.yml`，加上 `spring.autoconfigure.exclude` 排除 `RedisAutoConfiguration` 與 `RedisRepositoriesAutoConfiguration`；dev／default 皆不需 Redis
- **狀態**：已完成

### 2026-11-03 — B8 startRound 允許主講者或房主

- **改哪裡**：`GameService.startRound`
- **為什麼**：前端由當前回合主講者開下一輪，後端卻 `requireHost`，非房主主講者會失敗卡關
- **怎麼改**：有最近一輪時改呼叫既有 `requireSpeakerOrHost`；尚無回合（第一輪）仍僅房主
- **狀態**：已完成

### 2026-11-03 — B10 reveal 後自動 finishRound

- **改哪裡**：新增 `AutoFinishRoundScheduler`；`GameService.revealResult` / `finishRound`；註解 `VotingTimeoutScheduler`
- **為什麼**：逾時只 reveal，計分需房主手動 finish，房主離線會卡在 REVEALING
- **怎麼改**：reveal 成功後 4 秒自動 `finishRound(null)` 並廣播；手動 finish 時先 `cancel` 排程；`isFinished` 仍防重複計分
- **狀態**：已完成

### 2026-11-03 — B9 主講者斷線：無假圖則 abort

- **改哪裡**：`GameService.handlePlayerDisconnect`（新增 `abortRoundWithoutScoring`）
- **為什麼**：主講者斷線一律 `finishRound`，尚未選假圖時會丟錯導致回合卡死
- **怎麼改**：假圖為空則中止回合（不計分、回退 `currentRound`）；已有假圖才走原本 `finishRound`
- **狀態**：已完成

### 2026-11-03 — B11 votingEndsAt 改 Instant（UTC）

- **改哪裡**：`GameRound` / `GameRoundDTO` / `GameRoundRepository` / `GameService` / `GameRoundDtoMapper`；`Game.vue` 倒數解析
- **為什麼**：`LocalDateTime` 無時區，跨區客戶端解析倒數會錯位
- **怎麼改**：後端一律 `Instant`（REST/WS 同源 DTO，Jackson 輸出 ISO-8601 含 `Z`）；前端以 `Date`／epoch 對 `Date.now()` 計算剩餘秒數
- **狀態**：已完成

### 2026-11-03 — B6 WebSocket connect 失敗改為 throw + 提示

- **改哪裡**：`websocket.js`、`stores/game.js`、`Game.vue`、`Room.vue`
- **為什麼**：`connect()` 失敗回傳 `null`，呼叫端未處理，畫面靜默卡住
- **怎麼改**：失敗改 `throw new Error('WebSocket connect failed')`；`connectWebSocket` 清掉 connection 並上拋；Room／Game 用 toast「連線失敗，請重整」
- **狀態**：已完成

### 2026-11-03 — B12 同名重連原座位

- **改哪裡**：`RoomService`、`RoomController`、`ReconnectRoomRequest`、`game.js`、`Room.vue`
- **為什麼**：重整後 session 遺失，再用同名 join 會被擋，無法坐回原位
- **怎麼改**：`POST /rooms/reconnect` 以 playerId／暱稱認領座位；join 若 session 仍是該玩家則直接重連；Room 同名失敗時 confirm「是否要重連原座位」
- **狀態**：已完成
