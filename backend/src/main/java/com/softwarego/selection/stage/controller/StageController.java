package com.softwarego.selection.stage.controller;

import com.softwarego.selection.common.Result;
import com.softwarego.selection.stage.dto.CurrentStageResponse;
import com.softwarego.selection.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @GetMapping("/current")
    public Result<CurrentStageResponse> current() {
        return Result.ok(stageService.getCurrentStage());
    }
}
