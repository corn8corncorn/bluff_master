@echo off
chcp 65001 >nul
echo ========================================
echo   Bluff Master 後端服務啟動腳本
echo ========================================
echo.

REM 檢查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Java，請先安裝 Java 17+
    pause
    exit /b 1
)

REM 檢查 Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Maven，請先安裝 Maven 3.6+
    pause
    exit /b 1
)

echo [資訊] 正在啟動後端服務...
echo [資訊] 後端將在 http://localhost:8080 啟動
echo.

REM 啟動 Spring Boot
mvn spring-boot:run

pause

