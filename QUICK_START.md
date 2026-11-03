# 快速開始（約 3 分鐘）

前置：Java 17+、Node 18+、MySQL（資料庫 `bluff_master`）。**不需要 Redis。**

## Windows

雙擊專案根目錄的 `start-demo.bat`  
（自動起後端 JAR／Maven + 前端 + Cloudflare Tunnel）

## Linux / macOS

```bash
chmod +x start.sh
./start.sh
```

## 三指令版（clone 後手動）

```bash
git clone <你的-repo-URL> bluff_master && cd bluff_master
npm install && npm --prefix frontend install
./start.sh
```

Windows 第三步改為雙擊 `start-demo.bat`（或 `cd backend` 後 `start.bat`，另開視窗 `cd frontend && npm run dev`）。

## 驗證

瀏覽器打開：**http://localhost:3000**  
（本專案 Vite 埠為 3000，不是預設 5173）

應看到 Bluff Master 首頁，可「建立房間／加入房間」。

## 失敗時看 log

| 位置 | 怎麼看 |
|------|--------|
| 後端 | 跑後端的終端輸出；若有檔案則看 `backend/logs/` |
| 前端 | 瀏覽器 DevTools → Console |

更多說明見 [README.md](README.md)、[DEMO_GUIDE.md](DEMO_GUIDE.md)。
