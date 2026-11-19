package com.softwarego.selection.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简历上传配置。目录必须在 Web 根目录之外，不映射为静态资源。
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadProperties {

    private String dir;
    private long maxSize;
}
