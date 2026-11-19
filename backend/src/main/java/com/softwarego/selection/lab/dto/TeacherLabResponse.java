package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * GET /teacher/lab 响应。
 */
@JsonPropertyOrder({"submitted", "lab", "occupiedQuota", "remainingQuota"})
public record TeacherLabResponse(
        boolean submitted,
        TeacherLabItem lab,
        long occupiedQuota,
        Long remainingQuota
) {
}
