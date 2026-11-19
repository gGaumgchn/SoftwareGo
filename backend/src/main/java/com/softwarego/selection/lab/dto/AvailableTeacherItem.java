package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * 第二、三轮补录页顶部只读导师名额项。
 */
@Getter
@Setter
@JsonPropertyOrder({"teacherId", "teacherName", "labName", "location",
        "researchDirection", "plannedQuota", "remainingQuota"})
public class AvailableTeacherItem {

    private Long teacherId;
    private String teacherName;
    private String labName;
    private String location;
    private String researchDirection;
    private Integer plannedQuota;
    private Long remainingQuota;
}
