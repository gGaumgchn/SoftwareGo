package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.admission.domain.AdmissionRoundState;

import java.time.LocalDateTime;

@JsonPropertyOrder({"serverTime", "roundNo", "roundState", "roundStart", "roundEnd",
        "labSubmitted", "plannedQuota", "admittedCount", "pendingCount",
        "remainingQuota", "canGrant"})
public record TeacherAdmissionSummaryResponse(
        LocalDateTime serverTime,
        Integer roundNo,
        AdmissionRoundState roundState,
        LocalDateTime roundStart,
        LocalDateTime roundEnd,
        boolean labSubmitted,
        Integer plannedQuota,
        long admittedCount,
        long pendingCount,
        Long remainingQuota,
        boolean canGrant
) {
}
