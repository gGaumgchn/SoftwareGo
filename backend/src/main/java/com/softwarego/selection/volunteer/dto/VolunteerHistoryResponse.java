package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /student/volunteers 响应。
 */
@JsonPropertyOrder({"serverTime", "list"})
public record VolunteerHistoryResponse(
        LocalDateTime serverTime,
        List<VolunteerApplicationResponse> list
) {
}
