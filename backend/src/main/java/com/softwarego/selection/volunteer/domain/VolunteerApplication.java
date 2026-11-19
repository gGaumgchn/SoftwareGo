package com.softwarego.selection.volunteer.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * volunteer_application 实体：每生每轮一行。
 */
@Getter
@Setter
@NoArgsConstructor
public class VolunteerApplication {

    private Long id;
    private Long studentId;
    private Integer roundNo;
    private Long resumeFileId;
    private Boolean intraLabAdjust;
    private Boolean collegeAdjust;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
