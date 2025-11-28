@echo off
chcp 65001 >nul
echo ============================================
echo   啟動本地開發伺服器與 Cloudflare Tunnel
echo ============================================
echo.

REM 檢查 cloudflared 是否安裝
where cloudflared >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 cloudflared，請先安裝 Cloudflare Tunnel
    echo.
    echo 安裝方式：
    echo 1. 下載：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/
    echo 2. 或使用 winget: winget install --id Cloudflare.cloudflared
    echo.
    pause
    exit /b 1
)

REM 檢查 Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Node.js，請先安裝 Node.js 18+
    pause
    exit /b 1
)

echo ============================================
echo   步驟 1: 啟動後端服務 (端口 8080)
echo ============================================
echo.
echo 檢查後端服務是否已在 http://localhost:8080 運行...
echo.
echo 如果後端未啟動，有兩種方式：
echo   1. 使用 start-jar.bat（推薦，無需 Maven）
echo   2. 手動執行: cd backend ^&^& start-jar.bat
echo.
echo 等待 3 秒後繼續...
timeout /t 3 /nobreak >nul

echo.
echo ============================================
echo   步驟 2: 啟動前端開發伺服器 (端口 3000)
echo ============================================
echo.

cd frontend

REM 檢查是否已安裝依賴
if not exist "node_modules" (
    echo [資訊] 首次啟動，正在安裝依賴...
    call npm install
    echo.
)

echo [資訊] 正在啟動前端服務...
echo [資訊] 前端將在 http://localhost:3000 啟動
echo.

REM 在背景啟動前端開發伺服器
start "Bluff Master Frontend" cmd /k "npm run dev"

REM 等待前端啟動
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo   步驟 3: 啟動 Cloudflare Tunnel
echo ============================================
echo.
echo [資訊] 正在建立 Cloudflare Tunnel...
echo [資訊] 這將創建一個公開的 URL，讓其他人可以訪問您的本地開發伺服器
echo.

cd ..

REM 啟動 Cloudflare Tunnel（只暴露前端，前端會自動代理後端）
REM 前端已配置代理，會將 /api 和 /api/ws (WebSocket) 轉發到後端
cloudflared tunnel --url http://localhost:3000

pause

