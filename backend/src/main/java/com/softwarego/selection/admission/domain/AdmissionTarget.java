package com.softwarego.selection.admission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 导师视角的申请目标，带轮次与评分信息。
 */
@Getter
@Setter
@NoArgsConstructor
public class AdmissionTarget {

    private Long id;
    private Long applicationId;
    private Long teacherId;
    private Long studentId;
    private Integer roundNo;
    private BigDecimal score;
    private Integer tieOrder;
    private LocalDateTime scoredAt;
}
