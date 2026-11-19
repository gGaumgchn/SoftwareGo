package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 导师本人实训室信息。
 */
@JsonPropertyOrder({"id", "teacherId", "teacherName", "labName", "location",
        "plannedQuota", "researchDirection", "contact", "resumeFileId",
        "resumeFileName", "updatedAt"})
public record TeacherLabItem(
        Long id,
        Long teacherId,
        String teacherName,
        String labName,
        String location,
        Integer plannedQuota,
        String researchDirection,
        String contact,
        Long resumeFileId,
        String resumeFileName,
        LocalDateTime updatedAt
) {
}
