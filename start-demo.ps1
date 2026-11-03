# 啟動本地開發伺服器與 Cloudflare Tunnel
# PowerShell 版本 — 對齊 start-demo.bat：自動起後端（JAR 優先否則 Maven）+ 前端 + Tunnel

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  啟動本地開發伺服器與 Cloudflare Tunnel" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 cloudflared 是否安裝
$cloudflaredPath = Get-Command cloudflared -ErrorAction SilentlyContinue
if (-not $cloudflaredPath) {
    Write-Host "[錯誤] 未找到 cloudflared，請先安裝 Cloudflare Tunnel" -ForegroundColor Red
    Write-Host ""
    Write-Host "安裝方式："
    Write-Host "1. 下載：https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/"
    Write-Host "2. 或使用 winget: winget install --id Cloudflare.cloudflared"
    Write-Host ""
    Read-Host "按 Enter 鍵退出"
    exit 1
}

# 檢查 Node.js
$nodePath = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodePath) {
    Write-Host "[錯誤] 未找到 Node.js，請先安裝 Node.js 18+" -ForegroundColor Red
    Read-Host "按 Enter 鍵退出"
    exit 1
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  步驟 1: 啟動後端服務 (端口 8080)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location backend
$jarPath = "target/bluff-master-backend-1.0.0.jar"
if (Test-Path $jarPath) {
    Write-Host "[資訊] 使用已編譯的 JAR 文件啟動..." -ForegroundColor Green
    $backendProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "java -Dspring.profiles.active=dev -jar target/bluff-master-backend-1.0.0.jar" -PassThru
} else {
    Write-Host "[資訊] JAR 文件不存在，嘗試使用 Maven 啟動..." -ForegroundColor Yellow
    $backendProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run -Dspring-boot.run.profiles=dev" -PassThru
}
Set-Location ..

Write-Host "[資訊] 等待後端啟動（10秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  步驟 2: 啟動前端開發伺服器 (端口 3000)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location frontend

# 檢查是否已安裝依賴
if (-not (Test-Path "node_modules")) {
    Write-Host "[資訊] 首次啟動，正在安裝依賴..." -ForegroundColor Yellow
    npm install
    Write-Host ""
}

Write-Host "[資訊] 正在啟動前端服務..." -ForegroundColor Green
Write-Host "[資訊] 前端將在 http://localhost:3000 啟動" -ForegroundColor Green
Write-Host ""

# 在背景啟動前端開發伺服器
$frontendProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "npm run dev" -PassThru
Set-Location ..

# 等待前端啟動
Start-Sleep -Seconds 5

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  步驟 3: 啟動 Cloudflare Tunnel" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[資訊] 正在建立 Cloudflare Tunnel..." -ForegroundColor Green
Write-Host "[資訊] 這將創建一個公開的 URL，讓其他人可以訪問您的本地開發伺服器" -ForegroundColor Green
Write-Host "[資訊] 前端會自動代理後端 API 和 WebSocket 連接" -ForegroundColor Green
Write-Host ""

# 啟動 Cloudflare Tunnel（只暴露前端，前端會自動代理後端）
cloudflared tunnel --url http://localhost:3000

# 清理：當 Cloudflare Tunnel 停止時，也停止前端／後端視窗
Stop-Process -Id $frontendProcess.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
