package com.softwarego.selection.offer.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * offer 实体：导师发放的拟录取记录。
 */
@Getter
@Setter
@NoArgsConstructor
public class Offer {

    private Long id;
    private Long teacherId;
    private Long studentId;
    private Long applicationTargetId;
    private Integer roundNo;
    private OfferStatus status;
    private LocalDateTime confirmDeadline;
    private LocalDateTime grantedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime abandonedAt;
    private AbandonReason abandonReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
