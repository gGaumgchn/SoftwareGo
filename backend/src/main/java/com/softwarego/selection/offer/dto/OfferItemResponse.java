package com.softwarego.selection.offer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.offer.domain.AbandonReason;
import com.softwarego.selection.offer.domain.OfferStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * GET /student/offers 列表项。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"offerId", "teacherId", "teacherName", "labName", "roundNo",
        "status", "confirmDeadline", "grantedAt", "confirmedAt", "abandonedAt",
        "abandonReason", "canConfirm"})
public class OfferItemResponse {

    private Long offerId;
    private Long teacherId;
    private String teacherName;
    private String labName;
    private Integer roundNo;
    private OfferStatus status;
    private LocalDateTime confirmDeadline;
    private LocalDateTime grantedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime abandonedAt;
    private AbandonReason abandonReason;
    private boolean canConfirm;
}
