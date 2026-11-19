package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /student/volunteers 响应。
 */
@JsonPropertyOrder({"applicationId", "roundNo", "submittedAt", "intraLabAdjust",
        "collegeAdjust", "targets", "message"})
public record VolunteerSubmitResponse(
        Long applicationId,
        Integer roundNo,
        LocalDateTime submittedAt,
        boolean intraLabAdjust,
        boolean collegeAdjust,
        List<VolunteerTargetResponse> targets,
        String message
) {
}
