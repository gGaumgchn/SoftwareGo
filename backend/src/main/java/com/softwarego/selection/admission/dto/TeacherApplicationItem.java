package com.softwarego.selection.admission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.offer.domain.OfferStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 导师申请列表项。只包含本导师名下目标，不返回学生其他志愿。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"targetId", "applicationId", "studentId", "staffNo", "studentName",
        "preferenceNo", "score", "tieOrder", "scoredAt", "resumeFileId", "resumeFileName",
        "offerId", "offerStatus", "confirmDeadline", "studentAdmitted",
        "scoreEditable", "tieOrderEditable"})
public class TeacherApplicationItem {

    private Long targetId;
    private Long applicationId;
    private Long studentId;
    private String staffNo;
    private String studentName;
    private Integer preferenceNo;
    private BigDecimal score;
    private Integer tieOrder;
    private LocalDateTime scoredAt;
    private Long resumeFileId;
    private String resumeFileName;
    private Long offerId;
    private OfferStatus offerStatus;
    private LocalDateTime confirmDeadline;
    private boolean studentAdmitted;
    private boolean scoreEditable;
    private boolean tieOrderEditable;
}
