package com.softwarego.selection.volunteer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 志愿目标展示：只含输入框序号、导师与实训室，不返回分数/拟录取状态。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"preferenceNo", "teacherId", "teacherName", "labName"})
public class VolunteerTargetResponse {

    private Integer preferenceNo;
    private Long teacherId;
    private String teacherName;
    private String labName;
}
