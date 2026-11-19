package com.softwarego.selection.stage.controller;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.PageResult;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.stage.dto.AnnouncementRetryRequest;
import com.softwarego.selection.stage.dto.AnnouncementRetryResponse;
import com.softwarego.selection.stage.dto.ManagerStageResponse;
import com.softwarego.selection.stage.dto.StageChangeResponse;
import com.softwarego.selection.stage.dto.StageSaveRequest;
import com.softwarego.selection.stage.dto.StageSaveResponse;
import com.softwarego.selection.stage.service.StageConfigService;
import com.softwarego.selection.stage.service.StageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerStageController {

    private final StageService stageService;
    private final StageConfigService stageConfigService;

    @GetMapping("/stages")
    public Result<ManagerStageResponse> stages() {
        return Result.ok(stageService.getManagerStages());
    }

    @PutMapping("/stages")
    public Result<StageSaveResponse> save(@Valid @RequestBody StageSaveRequest request,
                                          @CurrentUser SessionUser sessionUser) {
        return Result.ok(stageConfigService.save(request, sessionUser.userId()));
    }

    @PostMapping("/stages/announcement")
    public Result<AnnouncementRetryResponse> retryAnnouncement(
            @Valid @RequestBody AnnouncementRetryRequest request) {
        return Result.ok(stageConfigService.retryAnnouncement(request.changeNo()));
    }

    @GetMapping("/stage-changes")
    public Result<PageResult<StageChangeResponse>> stageChanges(
            @RequestParam(required = false) AnnouncementStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(stageConfigService.listChanges(status, page, pageSize));
    }
}
