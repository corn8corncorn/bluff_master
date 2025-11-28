# 安裝 Maven 指南

如果您還沒有安裝 Maven，可以使用以下方法安裝。

## Windows

### 方法 1: 使用 Chocolatey（推薦）

```powershell
# 如果還沒有安裝 Chocolatey，先安裝它
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 安裝 Maven
choco install maven
```

### 方法 2: 使用 winget

```cmd
winget install Apache.Maven
```

### 方法 3: 手動安裝

1. 下載 Maven：https://maven.apache.org/download.cgi
   - 選擇 `apache-maven-3.x.x-bin.zip`
2. 解壓縮到 `C:\Program Files\Apache\maven`
3. 添加環境變數：
   - 變數名：`MAVEN_HOME`
   - 變數值：`C:\Program Files\Apache\maven`
4. 在 `Path` 環境變數中添加：`%MAVEN_HOME%\bin`
5. 重新開啟終端，驗證安裝：
   ```cmd
   mvn -version
   ```

## macOS

### 使用 Homebrew（推薦）

```bash
brew install maven
```

### 驗證安裝

```bash
mvn -version
```

## Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install maven
```

## 驗證安裝

安裝完成後，在任何終端執行：

```bash
mvn -version
```

應該會顯示 Maven 版本信息。

## 注意事項

- **已編譯的 JAR 文件**：如果您已經有編譯好的 JAR 文件（在 `target/` 目錄），可以直接使用 `start-jar.bat` 運行，無需安裝 Maven。
- **使用 IDE**：如果您使用 Eclipse 或 IntelliJ IDEA，它們通常內建 Maven 支援，可以直接編譯和運行。

