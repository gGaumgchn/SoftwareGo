package com.softwarego.selection.offer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * GET /student/admission 响应。
 */
@JsonPropertyOrder({"admitted", "admission"})
public record AdmissionResultResponse(
        boolean admitted,
        AdmissionResponse admission
) {
}
