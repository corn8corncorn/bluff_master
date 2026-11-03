#!/bin/bash
# Bluff Master 後端啟動腳本
#
# 執行權限（首次）：
#   chmod +x backend/start.sh
#   ./backend/start.sh
# 或於 backend 目錄內：chmod +x start.sh && ./start.sh

echo "========================================"
echo "  Bluff Master 後端服務啟動腳本"
echo "========================================"
echo ""

# 檢查 Java
if ! command -v java &> /dev/null; then
    echo "[錯誤] 未找到 Java，請先安裝 Java 17+"
    exit 1
fi

# 檢查是否有 JAR 文件
if [ -f "target/bluff-master-backend-1.0.0.jar" ]; then
    echo "[資訊] 發現已編譯的 JAR 文件，將直接運行..."
    echo "[資訊] 正在啟動後端服務（使用 dev profile）..."
    echo "[資訊] 後端將在 http://localhost:8080 啟動"
    echo "[資訊] 使用配置文件：application-dev.yml"
    echo ""

    # 直接運行 JAR（使用 dev profile；-D 必須在 -jar 之前）
    java -Dspring.profiles.active=dev -jar target/bluff-master-backend-1.0.0.jar
    exit $?
fi

# 檢查 Maven
if ! command -v mvn &> /dev/null; then
    echo "[錯誤] 未找到 Maven 且沒有已編譯的 JAR 文件"
    echo "[提示] 請選擇以下其中一種方式："
    echo "  1. 安裝 Maven 3.6+ 後重新執行此腳本"
    echo "  2. 使用 IDE（如 Eclipse/IntelliJ）編譯並運行"
    echo "  3. 或先編譯 JAR 後再執行此腳本"
    exit 1
fi

echo "[資訊] 正在啟動後端服務（使用 dev profile）..."
echo "[資訊] 後端將在 http://localhost:8080 啟動"
echo "[資訊] 使用配置文件：application-dev.yml"
echo ""

# 使用 Maven 啟動 Spring Boot（使用 dev profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
