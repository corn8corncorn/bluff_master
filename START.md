# 此文件已過時

**START.md 已合併到 [README.md](README.md) 與 [啟動說明.md](啟動說明.md)。**  
請改看上述兩份正本；本頁僅保留轉導，避免舊連結斷掉。

---

## 前置需求（摘要）

- **Java 17+**
- **Node.js 18+**
- **MySQL 8.0+**
- **不需要 Redis**（B5：預設已排除 Redis auto-config，程式未使用）

詳細說明見 [啟動說明.md](啟動說明.md)。

## 啟動方式（摘要）

| 系統 | 做法 |
|------|------|
| **Windows** | 雙擊專案根目錄的 `start-demo.bat` |
| **Linux／macOS** | `chmod +x start.sh && ./start.sh` |

完整說明（含手動啟動、驗證、故障排除）請看 README 的「啟動方式」章節：  
→ [README.md#啟動方式](README.md#啟動方式)

## 埠號

- 前端：`http://localhost:3000`
- 後端：`http://localhost:8080`（API context-path：`/api`）

與 [README.md](README.md)、[DEMO_GUIDE.md](DEMO_GUIDE.md) 一致。

## 其他文件

- [QUICK_START.md](QUICK_START.md) — 3 分鐘版
- [啟動說明.md](啟動說明.md) — 本地啟動完整版
- [如何分享給其他人.md](如何分享給其他人.md) — Tunnel 分享
- [BUGFIX_LOG.md](BUGFIX_LOG.md) — 修復紀錄
