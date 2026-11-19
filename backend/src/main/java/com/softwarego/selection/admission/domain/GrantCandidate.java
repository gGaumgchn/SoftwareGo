package com.softwarego.selection.admission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 发放候选：已评分、未产生 offer、学生无最终录取。
 */
@Getter
@Setter
@NoArgsConstructor
public class GrantCandidate {

    private Long targetId;
    private Long applicationId;
    private Long studentId;
    private String staffNo;
    private String studentName;
    private BigDecimal score;
    private Integer tieOrder;
}
