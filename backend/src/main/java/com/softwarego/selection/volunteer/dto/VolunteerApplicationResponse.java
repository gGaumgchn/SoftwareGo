package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本轮/历史志愿申请展示。
 */
@JsonPropertyOrder({"applicationId", "roundNo", "submittedAt", "intraLabAdjust",
        "collegeAdjust", "resumeFileId", "resumeFileName", "targets"})
public record VolunteerApplicationResponse(
        Long applicationId,
        Integer roundNo,
        LocalDateTime submittedAt,
        boolean intraLabAdjust,
        boolean collegeAdjust,
        Long resumeFileId,
        String resumeFileName,
        List<VolunteerTargetResponse> targets
) {
}
