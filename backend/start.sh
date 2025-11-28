#!/bin/bash

echo "========================================"
echo "  Bluff Master 後端服務啟動腳本"
echo "========================================"
echo ""

# 檢查 Java
if ! command -v java &> /dev/null; then
    echo "[錯誤] 未找到 Java，請先安裝 Java 17+"
    exit 1
fi

# 檢查 Maven
if ! command -v mvn &> /dev/null; then
    echo "[錯誤] 未找到 Maven，請先安裝 Maven 3.6+"
    exit 1
fi

echo "[資訊] 正在啟動後端服務（使用 dev profile）..."
echo "[資訊] 後端將在 http://localhost:8080 啟動"
echo "[資訊] 使用配置文件：application-dev.yml"
echo ""

# 啟動 Spring Boot（使用 dev profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

