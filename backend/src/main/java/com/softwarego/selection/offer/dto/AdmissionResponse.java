package com.softwarego.selection.offer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.offer.domain.AdmissionType;

import java.time.LocalDateTime;

/**
 * 最终录取结果。
 */
@JsonPropertyOrder({"id", "teacherId", "teacherName", "labName", "location",
        "roundNo", "admissionType", "admittedAt"})
public record AdmissionResponse(
        Long id,
        Long teacherId,
        String teacherName,
        String labName,
        String location,
        Integer roundNo,
        AdmissionType admissionType,
        LocalDateTime admittedAt
) {
}
