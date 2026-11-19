package com.softwarego.selection.offer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.offer.domain.OfferStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /student/offers/{id}/confirm 响应。
 */
@JsonPropertyOrder({"offerId", "status", "confirmedAt", "finalAdmission", "abandonedOfferIds"})
public record ConfirmOfferResponse(
        Long offerId,
        OfferStatus status,
        LocalDateTime confirmedAt,
        AdmissionResponse finalAdmission,
        List<Long> abandonedOfferIds
) {
}
