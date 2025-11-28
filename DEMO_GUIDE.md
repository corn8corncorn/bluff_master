# 🌐 Bluff Master 遠端 DEMO 指南

本指南說明如何使用 Cloudflare Tunnel 讓其他人透過網路訪問您的本地開發伺服器進行 DEMO。

## 📋 前置需求

### 1. 安裝 Cloudflare Tunnel

#### Windows

**方法 A - 使用 winget（推薦）：**
```cmd
winget install --id Cloudflare.cloudflared
```

**方法 B - 手動下載：**
1. 訪問：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/
2. 下載 Windows 版本
3. 解壓縮並將 `cloudflared.exe` 加入系統 PATH

#### macOS
```bash
brew install cloudflare/cloudflare/cloudflared
```

#### Linux
```bash
# Ubuntu/Debian
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared-linux-amd64.deb

# 其他發行版請參考官方文檔
```

### 2. 驗證安裝

```bash
cloudflared --version
```

## 🚀 快速啟動

### 方法 1: 使用完整啟動腳本（推薦）

**Windows:**
```cmd
start-demo-full.bat
```

這個腳本會自動：
1. 啟動後端服務（端口 8080）
2. 啟動前端開發伺服器（端口 3000）
3. 建立 Cloudflare Tunnel 並顯示公開 URL

### 方法 2: 手動啟動

#### 步驟 1: 啟動後端

在終端中執行：
```bash
cd backend
mvn spring-boot:run
```

等待後端在 `http://localhost:8080` 啟動。

#### 步驟 2: 啟動前端

**開啟新的終端視窗**，執行：
```bash
cd frontend
npm install  # 如果還沒安裝依賴
npm run dev
```

等待前端在 `http://localhost:3000` 啟動。

#### 步驟 3: 啟動 Cloudflare Tunnel

**開啟另一個終端視窗**，執行：

**Windows:**
```cmd
start-demo.bat
```

**PowerShell:**
```powershell
.\start-demo.ps1
```

**或直接使用命令：**
```bash
cloudflared tunnel --url http://localhost:3000
```

## 📱 分享 DEMO URL

Cloudflare Tunnel 啟動後，會顯示類似以下的 URL：

```
Your quick Tunnel has been created! Visit it at (it may take some time to be reachable):
https://type-barry-treatment-quantities.trycloudflare.com
```

將此 URL 分享給其他人即可訪問您的本地 DEMO。

## ⚠️ 重要注意事項

### 1. URL 會變化
- 每次啟動 Cloudflare Tunnel 時，URL 都會不同
- 需要重新分享新的 URL

### 2. 連接會中斷
- 當您關閉終端或中斷連接時，URL 會失效
- 重新啟動 Tunnel 會生成新的 URL

### 3. 後端服務必須運行
- 確保後端服務（端口 8080）正在運行
- 如果後端未啟動，前端會無法正常工作

### 4. 僅用於 DEMO
- Cloudflare Tunnel 僅適合用於展示和測試
- 生產環境請使用正式部署方案

### 5. WebSocket 支持
- Cloudflare Tunnel 自動支持 WebSocket 連接
- 遊戲的即時通訊功能可以正常使用

## 🔧 故障排除

### 問題 1: 找不到 cloudflared 命令

**解決方法：**
- 確認已正確安裝 Cloudflare Tunnel
- 檢查是否已將 `cloudflared` 加入系統 PATH
- 重新開啟終端視窗

### 問題 2: 前端無法連接後端

**解決方法：**
- 確認後端服務正在運行（訪問 `http://localhost:8080/api` 測試）
- 檢查後端日誌是否有錯誤
- 確認端口沒有被其他程序占用

### 問題 3: Tunnel URL 無法訪問

**解決方法：**
- 等待幾秒鐘讓 Tunnel 完全建立連接
- 檢查本地防火牆是否阻擋連接
- 確認本地服務已正常啟動

### 問題 4: WebSocket 連接失敗

**解決方法：**
- Cloudflare Tunnel 應該自動支持 WebSocket
- 如果仍有問題，嘗試重新啟動 Tunnel
- 檢查瀏覽器控制台是否有錯誤訊息

## 📝 使用範例

### 完整啟動流程

```bash
# 終端 1: 啟動後端
cd backend
mvn spring-boot:run

# 終端 2: 啟動前端
cd frontend
npm run dev

# 終端 3: 啟動 Tunnel
cloudflared tunnel --url http://localhost:3000
```

### 分享給團隊成員

1. 啟動所有服務後，複製 Cloudflare Tunnel 生成的 URL
2. 分享 URL 給需要測試的團隊成員
3. 他們可以在任何設備（手機、平板、電腦）上訪問 DEMO

## 🎯 最佳實踐

1. **測試本地訪問**：在啟動 Tunnel 前，先確認本地 `http://localhost:3000` 可以正常訪問
2. **保持服務運行**：在 DEMO 期間，不要關閉後端、前端或 Tunnel 的終端視窗
3. **檢查日誌**：如果出現問題，檢查各服務的日誌輸出
4. **定期更新 URL**：如果 Tunnel 中斷，重新啟動並分享新的 URL

## 📚 相關資源

- [Cloudflare Tunnel 官方文檔](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [Bluff Master 項目文檔](./README.md)

