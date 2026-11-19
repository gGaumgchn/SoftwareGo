package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * GET /labs/{id} 详情。
 */
@JsonPropertyOrder({"id", "labName", "location", "teacherId", "teacherName",
        "researchDirection", "plannedQuota", "labTotalQuota", "contact",
        "resumeFileId", "resumeFileName", "updatedAt"})
public record LabDetailResponse(
        Long id,
        String labName,
        String location,
        Long teacherId,
        String teacherName,
        String researchDirection,
        Integer plannedQuota,
        Long labTotalQuota,
        String contact,
        Long resumeFileId,
        String resumeFileName,
        LocalDateTime updatedAt
) {
}
