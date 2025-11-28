@echo off
chcp 65001 >nul
echo ============================================
echo   完整啟動腳本 - 包含後端、前端和 Tunnel
echo ============================================
echo.

REM 檢查 cloudflared
where cloudflared >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 cloudflared
    echo 請安裝: winget install --id Cloudflare.cloudflared
    pause
    exit /b 1
)

REM 檢查 Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Node.js
    pause
    exit /b 1
)

echo [資訊] 啟動後端服務（使用 dev profile）...
cd backend
REM 優先使用 JAR 啟動（不需要 Maven），如果 JAR 不存在則使用 Maven
if exist "target\bluff-master-backend-1.0.0.jar" (
    echo [資訊] 使用已編譯的 JAR 文件啟動...
    start "Bluff Master Backend" cmd /k "java -jar -Dspring.profiles.active=dev target\bluff-master-backend-1.0.0.jar"
) else (
    echo [資訊] JAR 文件不存在，嘗試使用 Maven 啟動...
    start "Bluff Master Backend" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=dev"
)
cd ..

echo [資訊] 等待後端啟動（10秒）...
timeout /t 10 /nobreak >nul

echo [資訊] 啟動前端服務...
cd frontend
if not exist "node_modules" (
    echo [資訊] 安裝前端依賴...
    call npm install
)
start "Bluff Master Frontend" cmd /k "npm run dev"
cd ..

echo [資訊] 等待前端啟動（5秒）...
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo   啟動 Cloudflare Tunnel
echo ============================================
echo [資訊] 正在建立公開 URL...
echo [資訊] 請將生成的 URL 分享給其他人訪問 DEMO
echo [資訊] 注意：URL 會在每次啟動時變化
echo [資訊] 前端會自動代理後端 API 和 WebSocket 連接
echo.

cloudflared tunnel --url http://localhost:3000

pause

