package com.softwarego.selection.offer.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * final_admission 实体：每名学生唯一的最终录取去向。
 */
@Getter
@Setter
@NoArgsConstructor
public class FinalAdmission {

    private Long id;
    private Long studentId;
    private Long teacherId;
    private Integer roundNo;
    private AdmissionType admissionType;
    private Long sourceOfferId;
    private LocalDateTime admittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
