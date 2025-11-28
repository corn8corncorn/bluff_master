@echo off
chcp 65001 >nul
echo ========================================
echo   Bluff Master 後端服務啟動腳本（直接運行 JAR）
echo ========================================
echo.

REM 檢查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Java，請先安裝 Java 17+
    pause
    exit /b 1
)

REM 檢查 JAR 文件是否存在
if not exist "target\bluff-master-backend-1.0.0.jar" (
    echo [錯誤] 未找到 JAR 文件：target\bluff-master-backend-1.0.0.jar
    echo [提示] 請先使用 Maven 編譯專案，或使用 IDE 編譯
    echo [提示] 或安裝 Maven 後執行: mvn clean package
    pause
    exit /b 1
)

echo [資訊] 正在啟動後端服務（使用 dev profile）...
echo [資訊] 後端將在 http://localhost:8080 啟動
echo [資訊] 使用配置文件：application-dev.yml
echo.

REM 啟動 Spring Boot（使用 dev profile）
java -jar -Dspring.profiles.active=dev target\bluff-master-backend-1.0.0.jar

pause

