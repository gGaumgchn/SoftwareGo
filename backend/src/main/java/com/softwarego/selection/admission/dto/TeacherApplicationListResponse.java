package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.admission.domain.AdmissionRoundState;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({"serverTime", "roundNo", "roundState", "roundStart", "roundEnd",
        "canScore", "canGrant", "list"})
public record TeacherApplicationListResponse(
        LocalDateTime serverTime,
        Integer roundNo,
        AdmissionRoundState roundState,
        LocalDateTime roundStart,
        LocalDateTime roundEnd,
        boolean canScore,
        boolean canGrant,
        List<TeacherApplicationItem> list
) {
}
