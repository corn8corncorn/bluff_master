#!/bin/bash

echo "========================================"
echo "  Bluff Master 前端服務啟動腳本"
echo "========================================"
echo ""

# 檢查 Node.js
if ! command -v node &> /dev/null; then
    echo "[錯誤] 未找到 Node.js，請先安裝 Node.js 18+"
    exit 1
fi

# 檢查是否已安裝依賴
if [ ! -d "node_modules" ]; then
    echo "[資訊] 首次啟動，正在安裝依賴..."
    npm install
    echo ""
fi

echo "[資訊] 正在啟動前端服務..."
echo "[資訊] 前端將在 http://localhost:3000 啟動"
echo ""

# 啟動開發伺服器
npm run dev

