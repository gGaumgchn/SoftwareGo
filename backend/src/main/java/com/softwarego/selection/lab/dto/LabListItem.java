package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * GET /labs 列表项。labTotalQuota 为同 labName + location 的 plannedQuota 汇总。
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "labName", "location", "teacherId", "teacherName",
        "researchDirection", "plannedQuota", "labTotalQuota", "contact", "resumeFileId"})
public class LabListItem {

    private Long id;
    private String labName;
    private String location;
    private Long teacherId;
    private String teacherName;
    private String researchDirection;
    private Integer plannedQuota;
    private Long labTotalQuota;
    private String contact;
    private Long resumeFileId;
}
