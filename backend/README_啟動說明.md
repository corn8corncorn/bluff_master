# 後端啟動說明

## 快速啟動（無需 Maven）

如果您已經有編譯好的 JAR 文件，可以直接運行：

### Windows
```cmd
start-jar.bat
```

或手動執行：
```cmd
java -jar -Dspring.profiles.active=dev target\bluff-master-backend-1.0.0.jar
```

## 使用 Maven 啟動

### 方式 1: 使用啟動腳本（推薦）
```cmd
start.bat
```

### 方式 2: 手動命令
```cmd
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 配置文件說明

- **application.yml** - 默認配置（使用環境變數）
- **application-dev.yml** - 開發環境配置（包含資料庫帳密）

啟動時使用 `dev` profile 會自動讀取 `application-dev.yml` 中的配置。

## 如果沒有 Maven

1. **使用已編譯的 JAR**：直接執行 `start-jar.bat`
2. **安裝 Maven**：查看 `INSTALL_MAVEN.md` 了解安裝方法
3. **使用 IDE**：Eclipse 或 IntelliJ IDEA 可以直接編譯和運行

## 常見問題

### JAR 文件不存在？
如果 `target/bluff-master-backend-1.0.0.jar` 不存在，需要先編譯：
- 使用 Maven: `mvn clean package`
- 或使用 IDE 編譯

### 資料庫連接失敗？
確認 `application-dev.yml` 中的資料庫配置是否正確：
- 用戶名: `root`
- 密碼: `Abc123!@#`
- 資料庫: `bluff_master`

