package com.softwarego.selection.announcement.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * GET /announcements 列表项。summary 由正文派生，不单独存储。
 */
@JsonPropertyOrder({"id", "title", "summary", "publishedAt"})
public record AnnouncementSummaryResponse(
        Long id,
        String title,
        String summary,
        LocalDateTime publishedAt
) {
}
