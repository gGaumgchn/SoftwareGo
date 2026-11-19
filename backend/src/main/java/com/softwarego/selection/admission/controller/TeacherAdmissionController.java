package com.softwarego.selection.admission.controller;

import com.softwarego.selection.admission.dto.GrantOfferRequest;
import com.softwarego.selection.admission.dto.GrantOfferResponse;
import com.softwarego.selection.admission.dto.ScoreUpdateRequest;
import com.softwarego.selection.admission.dto.ScoreUpdateResponse;
import com.softwarego.selection.admission.dto.TeacherAdmissionSummaryResponse;
import com.softwarego.selection.admission.dto.TeacherApplicationListResponse;
import com.softwarego.selection.admission.dto.TieOrderUpdateRequest;
import com.softwarego.selection.admission.dto.TieOrderUpdateResponse;
import com.softwarego.selection.admission.service.AdmissionService;
import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherAdmissionController {

    private final AdmissionService admissionService;

    @GetMapping("/applications")
    public Result<TeacherApplicationListResponse> applications(
            @RequestParam(required = false) Integer roundNo,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(admissionService.getApplications(sessionUser.userId(), roundNo));
    }

    @PutMapping("/applications/{id}/score")
    public Result<ScoreUpdateResponse> updateScore(
            @PathVariable Long id,
            @RequestBody(required = false) ScoreUpdateRequest request,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(admissionService.updateScore(sessionUser.userId(), id, request));
    }

    @PutMapping("/applications/{id}/order")
    public Result<TieOrderUpdateResponse> updateTieOrder(
            @PathVariable Long id,
            @RequestBody(required = false) TieOrderUpdateRequest request,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(admissionService.updateTieOrder(sessionUser.userId(), id, request));
    }

    @GetMapping("/admissions")
    public Result<TeacherAdmissionSummaryResponse> summary(
            @RequestParam(required = false) Integer roundNo,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(admissionService.getSummary(sessionUser.userId(), roundNo));
    }

    @PostMapping("/admissions/grant")
    public Result<GrantOfferResponse> grant(
            @RequestBody(required = false) GrantOfferRequest request,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(admissionService.grant(sessionUser.userId(), request));
    }
}
