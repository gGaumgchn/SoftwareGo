package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonPropertyOrder({"targetId", "score", "tieOrder", "updatedAt"})
public record TieOrderUpdateResponse(
        Long targetId,
        BigDecimal score,
        Integer tieOrder,
        LocalDateTime updatedAt
) {
}
