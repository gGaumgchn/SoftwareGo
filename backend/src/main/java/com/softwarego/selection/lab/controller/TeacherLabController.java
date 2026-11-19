package com.softwarego.selection.lab.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.lab.dto.LabInfoSaveRequest;
import com.softwarego.selection.lab.dto.TeacherLabResponse;
import com.softwarego.selection.lab.dto.TeacherLabSaveResponse;
import com.softwarego.selection.lab.service.LabInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/lab")
@RequiredArgsConstructor
public class TeacherLabController {

    private final LabInfoService labInfoService;

    @GetMapping
    public Result<TeacherLabResponse> get(@CurrentUser SessionUser sessionUser) {
        return Result.ok(labInfoService.getTeacherLab(sessionUser.userId()));
    }

    @PutMapping
    public Result<TeacherLabSaveResponse> save(
            @Valid @RequestBody LabInfoSaveRequest request,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(labInfoService.save(sessionUser.userId(), request));
    }
}
