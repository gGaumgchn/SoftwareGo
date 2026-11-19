package com.softwarego.selection.file.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.file.dto.ResumeFileUploadResponse;
import com.softwarego.selection.file.service.ResumeDownload;
import com.softwarego.selection.file.service.ResumeFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final ResumeFileService resumeFileService;

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ResumeFileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(resumeFileService.upload(file, sessionUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @CurrentUser SessionUser sessionUser) {
        ResumeDownload download = resumeFileService.download(id, sessionUser);
        String encodedName = URLEncoder.encode(
                download.originalName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }
}
