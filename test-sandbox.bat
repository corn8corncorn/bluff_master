@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ============================================
echo   測試沙盒：Docker MySQL + 後端 + 前端
echo ============================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Docker，請先安裝 Docker Desktop
    pause
    exit /b 1
)

echo [資訊] 啟動 MySQL（docker-compose）...
docker-compose up -d
if errorlevel 1 (
    docker compose up -d
    if errorlevel 1 (
        echo [錯誤] docker-compose / docker compose 啟動失敗
        pause
        exit /b 1
    )
)

echo [資訊] 等待 MySQL ready（10秒）...
timeout /t 10 /nobreak >nul

echo.
echo [資訊] 啟動後端服務（端口 8080）...
cd backend
if exist "target\bluff-master-backend-1.0.0.jar" (
    echo [資訊] 使用已編譯的 JAR 文件啟動...
    start "Bluff Master Backend" cmd /k java "-Dspring.profiles.active=dev" -jar target/bluff-master-backend-1.0.0.jar
) else (
    echo [資訊] JAR 不存在，使用 Maven 啟動...
    start "Bluff Master Backend" cmd /k mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
)
cd ..

echo [資訊] 等待後端啟動（10秒）...
timeout /t 10 /nobreak >nul

echo.
echo [資訊] 啟動前端開發伺服器（端口 3000）...
cd frontend
if not exist "node_modules" (
    echo [資訊] 首次啟動，正在安裝依賴...
    call npm install
)
start "Bluff Master Frontend" cmd /k "npm run dev"
cd ..

echo.
echo ============================================
echo   可開始測試：http://localhost:3000
echo   後端 API：http://localhost:8080/api
echo ============================================
echo.
pause
