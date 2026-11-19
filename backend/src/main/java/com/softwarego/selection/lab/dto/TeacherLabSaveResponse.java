package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * PUT /teacher/lab 响应。
 */
@JsonPropertyOrder({"submitted", "lab", "occupiedQuota", "remainingQuota", "message"})
public record TeacherLabSaveResponse(
        boolean submitted,
        TeacherLabItem lab,
        long occupiedQuota,
        Long remainingQuota,
        String message
) {
}
