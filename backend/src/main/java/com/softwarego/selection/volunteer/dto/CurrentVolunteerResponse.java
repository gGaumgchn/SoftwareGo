package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.volunteer.domain.BlockedReason;
import com.softwarego.selection.volunteer.domain.RoundState;

import java.time.LocalDateTime;

/**
 * GET /student/volunteers/current 响应。
 */
@JsonPropertyOrder({"serverTime", "roundNo", "roundState", "roundStart", "roundEnd",
        "admitted", "canSubmit", "blockedReason", "availableTeacherCount", "application"})
public record CurrentVolunteerResponse(
        LocalDateTime serverTime,
        Integer roundNo,
        RoundState roundState,
        LocalDateTime roundStart,
        LocalDateTime roundEnd,
        boolean admitted,
        boolean canSubmit,
        BlockedReason blockedReason,
        int availableTeacherCount,
        VolunteerApplicationResponse application
) {
}
