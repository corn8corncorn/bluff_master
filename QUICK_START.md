# 快速啟動指南

## 🚀 一鍵啟動（Windows）

### 方法 1: 使用啟動腳本

1. **啟動後端**：雙擊 `backend/start.bat`
2. **啟動前端**：雙擊 `frontend/start.bat`（在新的命令提示字元視窗）

### 方法 2: 使用命令列

開啟兩個命令提示字元視窗：

**視窗 1 - 後端：**
```cmd
cd backend
mvn spring-boot:run
```

**視窗 2 - 前端：**
```cmd
cd frontend
npm install
npm run dev
```

## 📋 啟動前檢查清單

- [ ] MySQL 已安裝並運行
- [ ] Redis 已安裝並運行
- [ ] Java 17+ 已安裝
- [ ] Maven 3.6+ 已安裝
- [ ] Node.js 18+ 已安裝
- [ ] 資料庫 `bluff_master` 已建立

## ⚡ 快速驗證

啟動後訪問：
- 前端：http://localhost:3000
- 後端 API：http://localhost:8080/api

## 🔧 如果遇到問題

請查看 [START.md](START.md) 獲取詳細的故障排除指南。

