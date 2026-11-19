package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonPropertyOrder({"targetId", "score", "tieOrder", "scoredAt"})
public record ScoreUpdateResponse(
        Long targetId,
        BigDecimal score,
        Integer tieOrder,
        LocalDateTime scoredAt
) {
}
