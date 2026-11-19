package com.softwarego.selection.lab.controller;

import com.softwarego.selection.common.Result;
import com.softwarego.selection.lab.dto.LabDetailResponse;
import com.softwarego.selection.lab.dto.LabListResponse;
import com.softwarego.selection.lab.service.LabInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabInfoService labInfoService;

    @GetMapping
    public Result<LabListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String researchDirection,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(labInfoService.list(keyword, researchDirection, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<LabDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(labInfoService.detail(id));
    }
}
