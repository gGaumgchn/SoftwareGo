package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"roundNo", "grantedCount", "offers", "remainingQuota", "message"})
public record GrantOfferResponse(
        Integer roundNo,
        int grantedCount,
        List<GrantedOfferItem> offers,
        long remainingQuota,
        String message
) {
}
