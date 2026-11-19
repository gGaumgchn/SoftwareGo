package com.softwarego.selection.announcement.controller;

import com.softwarego.selection.announcement.dto.AnnouncementDetailResponse;
import com.softwarego.selection.announcement.dto.AnnouncementSummaryResponse;
import com.softwarego.selection.announcement.service.AnnouncementService;
import com.softwarego.selection.common.PageResult;
import com.softwarego.selection.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public Result<PageResult<AnnouncementSummaryResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(announcementService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AnnouncementDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(announcementService.detail(id));
    }
}
