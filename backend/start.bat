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

REM 檢查是否有 JAR 文件
if exist "target\bluff-master-backend-1.0.0.jar" (
    echo [資訊] 發現已編譯的 JAR 文件，將直接運行...
    echo [資訊] 正在啟動後端服務（使用 dev profile）...
    echo [資訊] 後端將在 http://localhost:8080 啟動
    echo [資訊] 使用配置文件：application-dev.yml
    echo.
    
    REM 直接運行 JAR（使用 dev profile）
    java -jar -Dspring.profiles.active=dev target\bluff-master-backend-1.0.0.jar
    goto :end
)

REM 檢查 Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 未找到 Maven 且沒有已編譯的 JAR 文件
    echo [提示] 請選擇以下其中一種方式：
    echo   1. 安裝 Maven 3.6+ 後重新執行此腳本
    echo   2. 使用 IDE（如 Eclipse/IntelliJ）編譯並運行
    echo   3. 或使用 start-jar.bat 運行已編譯的 JAR（如果存在）
    pause
    exit /b 1
)

echo [資訊] 正在啟動後端服務（使用 dev profile）...
echo [資訊] 後端將在 http://localhost:8080 啟動
echo [資訊] 使用配置文件：application-dev.yml
echo.

REM 使用 Maven 啟動 Spring Boot（使用 dev profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

:end

pause

