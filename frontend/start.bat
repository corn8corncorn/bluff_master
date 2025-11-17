@echo off
chcp 65001 >nul
echo ========================================
echo   Bluff Master 前端服務啟動腳本
echo ========================================
echo.

REM 檢查 Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Node.js，請先安裝 Node.js 18+
    pause
    exit /b 1
)

REM 檢查是否已安裝依賴
if not exist "node_modules" (
    echo [資訊] 首次啟動，正在安裝依賴...
    call npm install
    echo.
)

echo [資訊] 正在啟動前端服務...
echo [資訊] 前端將在 http://localhost:3000 啟動
echo.

REM 啟動開發伺服器
npm run dev

pause

