package com.softwarego.selection.file.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * POST /files/resumes 响应。
 */
@JsonPropertyOrder({"fileId", "originalName", "fileSize", "contentType", "uploadedAt"})
public record ResumeFileUploadResponse(
        Long fileId,
        String originalName,
        Long fileSize,
        String contentType,
        LocalDateTime uploadedAt
) {
}
