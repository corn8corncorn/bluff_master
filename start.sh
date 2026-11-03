#!/bin/bash
# Bluff Master 一鍵啟動（後端 + 前端 + Cloudflare Tunnel）
# 對齊根目錄 start-demo-full.bat
#
# 執行權限（首次）：
#   chmod +x start.sh
#   ./start.sh
#
# 路徑一律使用 / ；環境變數請透過 npm 腳本的 cross-env 處理。

set -e
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

echo "============================================"
echo "  完整啟動腳本 - 包含後端、前端和 Tunnel"
echo "============================================"
echo ""

if ! command -v cloudflared &> /dev/null; then
    echo "[錯誤] 未找到 cloudflared"
    echo "請安裝：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "[錯誤] 未找到 Node.js，請先安裝 Node.js 18+"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "[錯誤] 未找到 Java，請先安裝 Java 17+"
    exit 1
fi

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
    echo ""
    echo "[資訊] 正在結束背景服務..."
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

echo "[資訊] 啟動後端服務（使用 dev profile）..."
cd "$ROOT_DIR/backend"
if [ -f "target/bluff-master-backend-1.0.0.jar" ]; then
    echo "[資訊] 使用已編譯的 JAR 文件啟動..."
    java -Dspring.profiles.active=dev -jar target/bluff-master-backend-1.0.0.jar &
    BACKEND_PID=$!
else
    if ! command -v mvn &> /dev/null; then
        echo "[錯誤] JAR 不存在且未找到 Maven"
        exit 1
    fi
    echo "[資訊] JAR 文件不存在，嘗試使用 Maven 啟動..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dev &
    BACKEND_PID=$!
fi
cd "$ROOT_DIR"

echo "[資訊] 等待後端啟動（10秒）..."
sleep 10

echo "[資訊] 啟動前端服務..."
cd "$ROOT_DIR/frontend"
if [ ! -d "node_modules" ]; then
    echo "[資訊] 安裝前端依賴..."
    npm install
fi
npm run dev &
FRONTEND_PID=$!
cd "$ROOT_DIR"

echo "[資訊] 等待前端啟動（5秒）..."
sleep 5

echo ""
echo "============================================"
echo "  啟動 Cloudflare Tunnel"
echo "============================================"
echo "[資訊] 正在建立公開 URL..."
echo "[資訊] 請將生成的 URL 分享給其他人訪問 DEMO"
echo "[資訊] 注意：URL 會在每次啟動時變化"
echo "[資訊] 前端會自動代理後端 API 和 WebSocket 連接"
echo ""

cloudflared tunnel --url http://localhost:3000
