#!/bin/bash
# Bluff Master 測試沙盒：Docker MySQL + 後端 + 前端
#
# 執行權限（首次）：
#   chmod +x test-sandbox.sh
#   ./test-sandbox.sh

set -e
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

echo "============================================"
echo "  測試沙盒：Docker MySQL + 後端 + 前端"
echo "============================================"
echo ""

if ! command -v docker &> /dev/null; then
    echo "[錯誤] 未找到 Docker，請先安裝 Docker"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "[錯誤] 未找到 Node.js，請先安裝 Node.js 18+"
    exit 1
fi

echo "[資訊] 啟動 MySQL（docker-compose）..."
if command -v docker-compose &> /dev/null; then
    docker-compose up -d
elif docker compose version &> /dev/null; then
    docker compose up -d
else
    echo "[錯誤] 未找到 docker-compose / docker compose"
    exit 1
fi

echo "[資訊] 等待 MySQL ready（10秒）..."
sleep 10

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

echo "[資訊] 啟動後端服務（端口 8080）..."
cd "$ROOT_DIR/backend"
if [ -f "target/bluff-master-backend-1.0.0.jar" ]; then
    echo "[資訊] 使用已編譯的 JAR 文件啟動..."
    java "-Dspring.profiles.active=dev" -jar target/bluff-master-backend-1.0.0.jar &
    BACKEND_PID=$!
else
    if ! command -v mvn &> /dev/null; then
        echo "[錯誤] JAR 不存在且未找到 Maven"
        exit 1
    fi
    echo "[資訊] JAR 不存在，使用 Maven 啟動..."
    mvn spring-boot:run "-Dspring-boot.run.profiles=dev" &
    BACKEND_PID=$!
fi
cd "$ROOT_DIR"

echo "[資訊] 等待後端啟動（10秒）..."
sleep 10

echo "[資訊] 啟動前端開發伺服器（端口 3000）..."
cd "$ROOT_DIR/frontend"
if [ ! -d "node_modules" ]; then
    echo "[資訊] 安裝前端依賴..."
    npm install
fi
npm run dev &
FRONTEND_PID=$!
cd "$ROOT_DIR"

echo ""
echo "============================================"
echo "  可開始測試：http://localhost:3000"
echo "  後端 API：http://localhost:8080/api"
echo "  按 Ctrl+C 結束後端／前端（MySQL 容器仍會保留）"
echo "============================================"
echo ""

wait "$FRONTEND_PID" "$BACKEND_PID"
