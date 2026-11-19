package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.offer.domain.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonPropertyOrder({"offerId", "studentId", "staffNo", "studentName", "score",
        "status", "confirmDeadline"})
public record GrantedOfferItem(
        Long offerId,
        Long studentId,
        String staffNo,
        String studentName,
        BigDecimal score,
        OfferStatus status,
        LocalDateTime confirmDeadline
) {
}
