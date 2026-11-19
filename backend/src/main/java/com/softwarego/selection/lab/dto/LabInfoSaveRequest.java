package com.softwarego.selection.lab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * PUT /teacher/lab 请求。不接受 teacherId，身份以会话为准。
 */
public record LabInfoSaveRequest(

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 100, message = "提交失败，请重试")
        String labName,

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 200, message = "提交失败，请重试")
        String location,

        @NotNull(message = "提交失败，请重试")
        @Min(value = 1, message = "提交失败，请重试")
        Integer plannedQuota,

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 64, message = "提交失败，请重试")
        String teacherName,

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 200, message = "提交失败，请重试")
        String researchDirection,

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 100, message = "提交失败，请重试")
        String contact,

        @NotNull(message = "提交失败，请重试")
        @Positive(message = "提交失败，请重试")
        Long resumeFileId
) {
}
