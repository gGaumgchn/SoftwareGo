package com.softwarego.selection.announcement.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * GET /announcements/{id} 详情。
 */
@JsonPropertyOrder({"id", "title", "content", "publisherId", "publisherName", "publishedAt"})
public record AnnouncementDetailResponse(
        Long id,
        String title,
        String content,
        Long publisherId,
        String publisherName,
        LocalDateTime publishedAt
) {
}
