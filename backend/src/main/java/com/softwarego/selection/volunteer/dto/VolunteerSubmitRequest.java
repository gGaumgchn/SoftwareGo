package com.softwarego.selection.volunteer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * POST /student/volunteers 请求。不接收 roundNo，轮次由服务端 stage 表判断。
 */
public record VolunteerSubmitRequest(

        @NotNull(message = "提交失败，请重试")
        List<String> teacherNames,

        @NotNull(message = "提交失败，请重试")
        Boolean intraLabAdjust,

        @NotNull(message = "提交失败，请重试")
        Boolean collegeAdjust,

        @NotNull(message = "提交失败，请重试")
        @Positive(message = "提交失败，请重试")
        Long resumeFileId
) {
}
