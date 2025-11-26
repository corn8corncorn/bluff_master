package com.bluffmaster.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${storage.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 僅在本地模式下配置靜態資源
        if ("local".equals(storageType)) {
            try {
                String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString().replace("\\", "/");
                String resourceLocation = "file:" + absolutePath + "/";
                
                // 注意：由於 server.servlet.context-path=/api，所以路徑應該是 /api/images/**
                // 但 ResourceHandler 會自動處理 context-path
                registry.addResourceHandler("/images/**")
                        .addResourceLocations(resourceLocation)
                        .setCachePeriod(3600); // 緩存 1 小時
                
                log.info("配置圖片資源路徑: {} -> {}", "/images/**", resourceLocation);
            } catch (Exception e) {
                log.error("配置圖片資源路徑失敗", e);
            }
        }
    }
}

