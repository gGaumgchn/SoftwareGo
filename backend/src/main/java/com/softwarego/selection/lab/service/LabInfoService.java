package com.softwarego.selection.lab.service;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.file.domain.ResumeFile;
import com.softwarego.selection.file.service.ResumeFileService;
import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.dto.AvailableTeacherItem;
import com.softwarego.selection.lab.dto.LabDetailResponse;
import com.softwarego.selection.lab.dto.LabInfoSaveRequest;
import com.softwarego.selection.lab.dto.LabListItem;
import com.softwarego.selection.lab.dto.LabListResponse;
import com.softwarego.selection.lab.dto.TeacherLabItem;
import com.softwarego.selection.lab.dto.TeacherLabResponse;
import com.softwarego.selection.lab.dto.TeacherLabSaveResponse;
import com.softwarego.selection.lab.mapper.LabInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LabInfoService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final LabInfoMapper labInfoMapper;
    private final QuotaService quotaService;
    private final ResumeFileService resumeFileService;

    public TeacherLabResponse getTeacherLab(Long teacherId) {
        LabInfo lab = labInfoMapper.findByTeacherId(teacherId);
        long occupied = quotaService.occupied(teacherId);
        if (lab == null) {
            return new TeacherLabResponse(false, null, occupied, null);
        }

        ResumeFile resumeFile = resumeFileService.findById(lab.getResumeFileId());
        return new TeacherLabResponse(
                true,
                toTeacherItem(lab, resumeFile),
                occupied,
                quotaService.remaining(lab.getPlannedQuota(), occupied));
    }

    @Transactional
    public TeacherLabSaveResponse save(Long teacherId, LabInfoSaveRequest request) {
        ResumeFile resumeFile = resumeFileService.requireTeacherResume(
                request.resumeFileId(), teacherId);

        // 锁定本人一行；并发首次提交由 uk_lab_teacher 兜底。
        LabInfo existing = labInfoMapper.findByTeacherIdForUpdate(teacherId);
        long occupied = quotaService.occupied(teacherId);
        if (request.plannedQuota() < occupied) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "计划招收人数不能小于当前已占用名额");
        }

        LabInfo lab = existing == null ? new LabInfo() : existing;
        lab.setTeacherId(teacherId);
        lab.setTeacherName(normalizeTeacherName(request.teacherName()));
        lab.setLabName(request.labName().trim());
        lab.setLocation(request.location().trim());
        lab.setPlannedQuota(request.plannedQuota());
        lab.setResearchDirection(request.researchDirection().trim());
        lab.setContact(request.contact().trim());
        lab.setResumeFileId(resumeFile.getId());

        if (existing == null) {
            labInfoMapper.insert(lab);
        } else {
            labInfoMapper.update(lab);
        }

        LabInfo saved = labInfoMapper.findByTeacherId(teacherId);
        return new TeacherLabSaveResponse(
                true,
                toTeacherItem(saved, resumeFile),
                occupied,
                quotaService.remaining(saved.getPlannedQuota(), occupied),
                "提交成功");
    }

    public LabListResponse list(String keyword, String researchDirection, int page, int pageSize) {
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeFilter(keyword);
        String normalizedDirection = normalizeFilter(researchDirection);
        long offset = (long) (page - 1) * pageSize;

        List<LabListItem> list = labInfoMapper.findPage(
                normalizedKeyword, normalizedDirection, offset, pageSize);
        long total = labInfoMapper.count(normalizedKeyword, normalizedDirection);
        List<String> researchDirections = labInfoMapper.findResearchDirections();
        return new LabListResponse(list, total, page, pageSize, researchDirections);
    }

    public LabDetailResponse detail(Long id) {
        LabInfo lab = labInfoMapper.findById(id);
        if (lab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        ResumeFile resumeFile = resumeFileService.findById(lab.getResumeFileId());
        long labTotalQuota = labInfoMapper.sumQuotaByLabNameAndLocation(
                lab.getLabName(), lab.getLocation());
        return new LabDetailResponse(
                lab.getId(),
                lab.getLabName(),
                lab.getLocation(),
                lab.getTeacherId(),
                lab.getTeacherName(),
                lab.getResearchDirection(),
                lab.getPlannedQuota(),
                labTotalQuota,
                lab.getContact(),
                lab.getResumeFileId(),
                resumeFile == null ? null : resumeFile.getOriginalName(),
                lab.getUpdatedAt());
    }

    public LabInfo findByTeacherId(Long teacherId) {
        return labInfoMapper.findByTeacherId(teacherId);
    }

    /**
     * 调剂导入时锁定导师名额行，避免并发导入突破 planned_quota。
     */
    @Transactional
    public LabInfo findByTeacherIdForUpdate(Long teacherId) {
        return labInfoMapper.findByTeacherIdForUpdate(teacherId);
    }

    public List<LabInfo> findByTeacherName(String teacherName) {
        return labInfoMapper.findByTeacherName(teacherName);
    }

    public List<AvailableTeacherItem> findAvailableTeachers(LocalDateTime now) {
        return labInfoMapper.findAvailableTeachers(now);
    }

    private TeacherLabItem toTeacherItem(LabInfo lab, ResumeFile resumeFile) {
        return new TeacherLabItem(
                lab.getId(),
                lab.getTeacherId(),
                lab.getTeacherName(),
                lab.getLabName(),
                lab.getLocation(),
                lab.getPlannedQuota(),
                lab.getResearchDirection(),
                lab.getContact(),
                lab.getResumeFileId(),
                resumeFile == null ? null : resumeFile.getOriginalName(),
                lab.getUpdatedAt());
    }

    private String normalizeTeacherName(String teacherName) {
        return WHITESPACE.matcher(teacherName.trim()).replaceAll(" ");
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "分页参数不合法");
        }
    }
}
