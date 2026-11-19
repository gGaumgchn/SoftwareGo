package com.softwarego.selection.lab.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * lab_info 实体：每位导师一行，不是共享实训室主数据。
 */
@Getter
@Setter
@NoArgsConstructor
public class LabInfo {

    private Long id;
    private Long teacherId;
    private String teacherName;
    private String labName;
    private String location;
    private Integer plannedQuota;
    private String researchDirection;
    private String contact;
    private Long resumeFileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
