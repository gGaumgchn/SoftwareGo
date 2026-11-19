package com.softwarego.selection.file.service;

import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.config.UploadProperties;
import com.softwarego.selection.file.domain.ResumeFile;
import com.softwarego.selection.file.domain.ResumePurpose;
import com.softwarego.selection.file.dto.ResumeFileUploadResponse;
import com.softwarego.selection.file.mapper.ResumeFileMapper;
import com.softwarego.selection.user.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeFileService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ResumeFileMapper resumeFileMapper;
    private final ResumeFileStorage resumeFileStorage;
    private final UploadProperties uploadProperties;
    private final Clock clock;

    /**
     * 上传 PDF 简历。磁盘写入成功后才插入数据库；数据库失败会清理已写文件。
     */
    @Transactional
    public ResumeFileUploadResponse upload(MultipartFile file, SessionUser sessionUser) {
        ResumePurpose purpose = switch (sessionUser.role()) {
            case STUDENT -> ResumePurpose.STUDENT_RESUME;
            case TEACHER -> ResumePurpose.TEACHER_RESUME;
            case MANAGER -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };

        String originalName = sanitizeOriginalName(file == null ? null : file.getOriginalFilename());
        validatePdf(file, originalName);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            log.error("读取简历文件失败: ownerId={}, purpose={}", sessionUser.userId(), purpose, exception);
            throw uploadFailed();
        }
        if (content.length > uploadProperties.getMaxSize()) {
            throw uploadFailed();
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        String storagePath = String.format("%04d/%02d/%s.pdf",
                now.getYear(), now.getMonthValue(), UUID.randomUUID());

        try {
            resumeFileStorage.store(storagePath, content);
        } catch (Exception exception) {
            log.error("简历写入磁盘失败: ownerId={}, purpose={}", sessionUser.userId(), purpose, exception);
            throw uploadFailed();
        }

        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setOwnerId(sessionUser.userId());
        resumeFile.setPurpose(purpose);
        resumeFile.setOriginalName(originalName);
        resumeFile.setStoragePath(storagePath);
        resumeFile.setContentType(PDF_CONTENT_TYPE);
        resumeFile.setFileSize((long) content.length);

        try {
            resumeFileMapper.insert(resumeFile);
        } catch (RuntimeException exception) {
            resumeFileStorage.delete(storagePath);
            log.error("简历元数据写入失败: ownerId={}, purpose={}", sessionUser.userId(), purpose, exception);
            throw uploadFailed();
        }

        return new ResumeFileUploadResponse(
                resumeFile.getId(), originalName, (long) content.length, PDF_CONTENT_TYPE, now);
    }

    public ResumeFile findById(Long id) {
        return id == null ? null : resumeFileMapper.findById(id);
    }

    public ResumeFile requireTeacherResume(Long fileId, Long teacherId) {
        ResumeFile resumeFile = findById(fileId);
        if (resumeFile == null
                || !teacherId.equals(resumeFile.getOwnerId())
                || resumeFile.getPurpose() != ResumePurpose.TEACHER_RESUME) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return resumeFile;
    }

    public ResumeFile requireStudentResume(Long fileId, Long studentId) {
        ResumeFile resumeFile = findById(fileId);
        if (resumeFile == null
                || !studentId.equals(resumeFile.getOwnerId())
                || resumeFile.getPurpose() != ResumePurpose.STUDENT_RESUME) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return resumeFile;
    }

    /**
     * 下载权限：导师简历所有登录用户可读；学生简历仅本人和收到其申请的导师可读。
     */
    public ResumeDownload download(Long id, SessionUser sessionUser) {
        ResumeFile resumeFile = findById(id);
        if (resumeFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        if (resumeFile.getPurpose() == ResumePurpose.STUDENT_RESUME) {
            boolean owner = resumeFile.getOwnerId().equals(sessionUser.userId());
            boolean teacherReceived = sessionUser.role() == Role.TEACHER
                    && resumeFileMapper.countTeacherApplicationTargets(
                            sessionUser.userId(), resumeFile.getOwnerId()) > 0;
            if (!owner && !teacherReceived) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        Path path = resumeFileStorage.resolve(resumeFile.getStoragePath());
        if (path == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new ResumeDownload(resource, resumeFile.getOriginalName(), contentLength);
    }

    private void validatePdf(MultipartFile file, String originalName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请上传 PDF 格式的简历");
        }
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请上传 PDF 格式的简历");
        }
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请上传 PDF 格式的简历");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw uploadFailed();
        }

        byte[] prefix;
        try (InputStream inputStream = file.getInputStream()) {
            prefix = inputStream.readNBytes(PDF_HEADER.length);
        } catch (IOException exception) {
            log.error("读取简历文件头失败: ownerId={}", "unknown", exception);
            throw uploadFailed();
        }
        if (prefix.length < PDF_HEADER.length) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请上传 PDF 格式的简历");
        }
        for (int index = 0; index < PDF_HEADER.length; index += 1) {
            if (prefix[index] != PDF_HEADER[index]) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请上传 PDF 格式的简历");
            }
        }
    }

    /**
     * 只保留文件名，去掉路径分量；磁盘名始终使用 UUID，原始文件名仅用于展示。
     */
    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "resume.pdf";
        }
        String normalized = originalName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        normalized = normalized.trim();
        if (normalized.isEmpty()) {
            normalized = "resume.pdf";
        }
        if (normalized.length() > 255) {
            normalized = normalized.substring(0, 255);
        }
        return normalized;
    }

    private BusinessException uploadFailed() {
        return new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "简历上传失败，请重试");
    }
}
