package com.softwarego.selection.file.service;

import org.springframework.core.io.Resource;

/**
 * 鉴权下载结果：Controller 负责设置 HTTP 头。
 */
public record ResumeDownload(Resource resource, String originalName, long contentLength) {
}
