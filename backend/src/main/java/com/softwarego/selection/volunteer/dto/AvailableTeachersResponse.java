package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.lab.dto.AvailableTeacherItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /student/available-teachers 响应。
 */
@JsonPropertyOrder({"serverTime", "roundNo", "list"})
public record AvailableTeachersResponse(
        LocalDateTime serverTime,
        Integer roundNo,
        List<AvailableTeacherItem> list
) {
}
