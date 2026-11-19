package com.softwarego.selection.volunteer.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.volunteer.dto.AvailableTeachersResponse;
import com.softwarego.selection.volunteer.dto.CurrentVolunteerResponse;
import com.softwarego.selection.volunteer.dto.VolunteerHistoryResponse;
import com.softwarego.selection.volunteer.dto.VolunteerSubmitRequest;
import com.softwarego.selection.volunteer.dto.VolunteerSubmitResponse;
import com.softwarego.selection.volunteer.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping("/volunteers")
    public Result<VolunteerHistoryResponse> history(
            @RequestParam(required = false) Integer roundNo,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(volunteerService.getHistory(sessionUser.userId(), roundNo));
    }

    @GetMapping("/volunteers/current")
    public Result<CurrentVolunteerResponse> current(@CurrentUser SessionUser sessionUser) {
        return Result.ok(volunteerService.getCurrent(sessionUser.userId()));
    }

    @GetMapping("/available-teachers")
    public Result<AvailableTeachersResponse> availableTeachers() {
        return Result.ok(volunteerService.getAvailableTeachers());
    }

    @PostMapping("/volunteers")
    public Result<VolunteerSubmitResponse> submit(
            @Valid @RequestBody VolunteerSubmitRequest request,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(volunteerService.submit(sessionUser.userId(), request));
    }
}
