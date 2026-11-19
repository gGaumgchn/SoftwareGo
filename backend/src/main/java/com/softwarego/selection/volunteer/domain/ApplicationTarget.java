package com.softwarego.selection.volunteer.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * application_target 实体：一次申请解析出的 1–3 个导师目标。
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationTarget {

    private Long id;
    private Long applicationId;
    private Long teacherId;
    private Integer preferenceNo;
    private BigDecimal score;
    private Integer tieOrder;
    private LocalDateTime scoredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
