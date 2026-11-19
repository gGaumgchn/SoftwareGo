package com.softwarego.selection.offer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /student/offers 响应。
 */
@JsonPropertyOrder({"serverTime", "list"})
public record OfferListResponse(
        LocalDateTime serverTime,
        List<OfferItemResponse> list
) {
}
