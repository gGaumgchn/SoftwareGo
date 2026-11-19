package com.softwarego.selection.stage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PUT /manager/stages 请求。阶段数量/编码/顺序等语义校验在 Service 中完成。
 */
public record StageSaveRequest(

        @NotBlank(message = "提交失败，请重试")
        @Size(max = 64, message = "提交失败，请重试")
        String changeNo,

        @Valid
        @NotNull(message = "提交失败，请重试")
        @Size(min = 7, max = 7, message = "提交失败，请重试")
        List<StageSaveItem> stages,

        @NotNull(message = "提交失败，请重试")
        Boolean publishAnnouncement,

        AnnouncementDraftRequest announcement
) {

    public record StageSaveItem(

            @NotBlank(message = "提交失败，请重试")
            String stageCode,

            @NotNull(message = "请填写完整的阶段时间")
            LocalDateTime startAt,

            @NotNull(message = "请填写完整的阶段时间")
            LocalDateTime endAt
    ) {
    }

    public record AnnouncementDraftRequest(String title, String content) {
    }
}
