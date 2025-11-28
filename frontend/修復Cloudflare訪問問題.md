# 修復 Cloudflare Tunnel 訪問問題

## 問題描述

使用 Cloudflare Tunnel 分享 URL 時，出現以下錯誤：

```
Blocked request. This host ("xxx.trycloudflare.com") is not allowed.
To allow this host, add "xxx.trycloudflare.com" to `server.allowedHosts` in vite.config.js.
```

## 解決方案

已在 `vite.config.js` 中添加配置，允許 Cloudflare Tunnel 的域名訪問。

## 重要：需要重新啟動前端服務

配置更改後，**必須重新啟動前端服務**才能生效！

### 步驟：

1. **停止當前的前端服務**
   - 在運行前端的終端視窗中按 `Ctrl+C`

2. **重新啟動前端服務**
   ```cmd
   cd frontend
   start.bat
   ```

3. **重新啟動 Cloudflare Tunnel**
   - 停止當前的 Tunnel（按 `Ctrl+C`）
   - 重新執行 `start-demo.bat`

4. **訪問新的 URL**
   - 使用新生成的 URL 訪問應用

## 配置說明

`vite.config.js` 中已添加：

```javascript
server: {
  host: '0.0.0.0', // 允許外部訪問
  allowedHosts: [
    'localhost',
    '.trycloudflare.com', // 允許所有 Cloudflare Tunnel 域名
    '.local'
  ]
}
```

這個配置會允許：
- ✅ `localhost` - 本地訪問
- ✅ `*.trycloudflare.com` - 所有 Cloudflare Tunnel 域名
- ✅ `*.local` - 本地網路域名

## 驗證

重新啟動後，訪問 Cloudflare Tunnel 的 URL 應該可以正常打開應用，不再出現 "Blocked request" 錯誤。

