package com.softwarego.selection.stage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /manager/stages/announcement 请求：只按 changeNo 重试公告。
 */
public record AnnouncementRetryRequest(

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 64, message = "提交失败，请重试")
        String changeNo
) {
}
