package com.softwarego.selection.volunteer.service;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.file.domain.ResumeFile;
import com.softwarego.selection.file.service.ResumeFileService;
import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.dto.AvailableTeacherItem;
import com.softwarego.selection.lab.service.LabInfoService;
import com.softwarego.selection.volunteer.domain.ApplicationTarget;
import com.softwarego.selection.volunteer.domain.BlockedReason;
import com.softwarego.selection.volunteer.domain.RoundState;
import com.softwarego.selection.volunteer.domain.VolunteerApplication;
import com.softwarego.selection.volunteer.domain.VolunteerRound;
import com.softwarego.selection.volunteer.dto.AvailableTeachersResponse;
import com.softwarego.selection.volunteer.dto.CurrentVolunteerResponse;
import com.softwarego.selection.volunteer.dto.VolunteerApplicationResponse;
import com.softwarego.selection.volunteer.dto.VolunteerHistoryResponse;
import com.softwarego.selection.volunteer.dto.VolunteerSubmitRequest;
import com.softwarego.selection.volunteer.dto.VolunteerSubmitResponse;
import com.softwarego.selection.volunteer.dto.VolunteerTargetResponse;
import com.softwarego.selection.volunteer.mapper.ApplicationTargetMapper;
import com.softwarego.selection.volunteer.mapper.VolunteerApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolunteerService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final VolunteerRoundResolver roundResolver;
    private final VolunteerApplicationMapper applicationMapper;
    private final ApplicationTargetMapper targetMapper;
    private final LabInfoService labInfoService;
    private final ResumeFileService resumeFileService;
    private final Clock clock;

    public CurrentVolunteerResponse getCurrent(Long studentId) {
        VolunteerRound round = roundResolver.resolve();
        boolean admitted = applicationMapper.countFinalAdmissions(studentId) > 0;
        List<AvailableTeacherItem> available = round.roundNo() == null
                ? List.of()
                : labInfoService.findAvailableTeachers(round.serverTime());
        VolunteerApplication application = round.roundNo() == null
                ? null
                : applicationMapper.findByStudentAndRound(studentId, round.roundNo());
        boolean processed = application != null
                && targetMapper.countProcessedTargets(application.getId()) > 0;
        BlockedReason blockedReason = resolveBlockedReason(round, admitted, available, processed);
        boolean canSubmit = blockedReason == null && round.state() == RoundState.OPEN;

        return new CurrentVolunteerResponse(
                round.serverTime(),
                round.roundNo(),
                round.state(),
                round.roundStart(),
                round.roundEnd(),
                admitted,
                canSubmit,
                blockedReason,
                round.roundNo() == null ? 0 : available.size(),
                application == null ? null : toApplicationResponse(application));
    }

    public VolunteerHistoryResponse getHistory(Long studentId, Integer roundNo) {
        if (roundNo != null && (roundNo < 1 || roundNo > 3)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "提交失败，请重试");
        }
        List<VolunteerApplicationResponse> list = applicationMapper
                .findByStudent(studentId, roundNo).stream()
                .map(this::toApplicationResponse)
                .toList();
        return new VolunteerHistoryResponse(now(), list);
    }

    public AvailableTeachersResponse getAvailableTeachers() {
        VolunteerRound round = roundResolver.resolve();
        if (round.state() != RoundState.OPEN) {
            return new AvailableTeachersResponse(round.serverTime(), null, List.of());
        }
        return new AvailableTeachersResponse(
                round.serverTime(),
                round.roundNo(),
                labInfoService.findAvailableTeachers(round.serverTime()));
    }

    @Transactional
    public VolunteerSubmitResponse submit(Long studentId, VolunteerSubmitRequest request) {
        VolunteerRound round = roundResolver.resolve();
        validateRoundOpen(round);

        if (applicationMapper.countFinalAdmissions(studentId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已确认录取，无需参加补录");
        }
        if (request == null) {
            throw invalidSubmit();
        }

        List<String> names = normalizeNames(request.teacherNames());
        if (names.isEmpty() || names.size() > 3) {
            throw invalidSubmit();
        }
        if (new HashSet<>(names).size() != names.size()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "导师姓名不能重复");
        }
        if (request.intraLabAdjust() == null || request.collegeAdjust() == null) {
            throw invalidSubmit();
        }

        List<AvailableTeacherItem> available = labInfoService.findAvailableTeachers(round.serverTime());
        if (round.roundNo() >= 2 && available.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "本轮暂无可填报名额");
        }

        resumeFileService.requireStudentResume(request.resumeFileId(), studentId);
        List<LabInfo> labs = resolveLabs(names);

        if (round.roundNo() >= 2) {
            Set<Long> availableTeacherIds = available.stream()
                    .map(AvailableTeacherItem::getTeacherId)
                    .collect(Collectors.toSet());
            for (LabInfo lab : labs) {
                if (!availableTeacherIds.contains(lab.getTeacherId())) {
                    throw new BusinessException(
                            ErrorCode.CONFLICT, "所填导师已无剩余名额，请重新选择");
                }
            }
        }

        LocalDateTime now = round.serverTime();
        VolunteerApplication existing = applicationMapper.findByStudentAndRoundForUpdate(
                studentId, round.roundNo());
        if (existing != null && targetMapper.countProcessedTargets(existing.getId()) > 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "本轮志愿已进入处理阶段，不能再修改");
        }

        VolunteerApplication application;
        if (existing != null) {
            application = existing;
            application.setResumeFileId(request.resumeFileId());
            application.setIntraLabAdjust(request.intraLabAdjust());
            application.setCollegeAdjust(request.collegeAdjust());
            application.setSubmittedAt(now);
            applicationMapper.update(application);
            targetMapper.deleteByApplicationId(application.getId());
        } else {
            application = new VolunteerApplication();
            application.setStudentId(studentId);
            application.setRoundNo(round.roundNo());
            application.setResumeFileId(request.resumeFileId());
            application.setIntraLabAdjust(request.intraLabAdjust());
            application.setCollegeAdjust(request.collegeAdjust());
            application.setSubmittedAt(now);
            applicationMapper.insert(application);
        }

        List<VolunteerTargetResponse> targets = new ArrayList<>(labs.size());
        for (int index = 0; index < labs.size(); index += 1) {
            LabInfo lab = labs.get(index);
            ApplicationTarget target = new ApplicationTarget();
            target.setApplicationId(application.getId());
            target.setTeacherId(lab.getTeacherId());
            target.setPreferenceNo(index + 1);
            targetMapper.insert(target);

            VolunteerTargetResponse response = new VolunteerTargetResponse();
            response.setPreferenceNo(index + 1);
            response.setTeacherId(lab.getTeacherId());
            response.setTeacherName(lab.getTeacherName());
            response.setLabName(lab.getLabName());
            targets.add(response);
        }

        // 只记录业务主键与数量，不记录简历、导师姓名等敏感/非必要内容。
        log.info("提交志愿: studentId={}, roundNo={}, applicationId={}, targetCount={}",
                studentId, round.roundNo(), application.getId(), targets.size());

        return new VolunteerSubmitResponse(
                application.getId(),
                round.roundNo(),
                now,
                request.intraLabAdjust(),
                request.collegeAdjust(),
                targets,
                "提交成功");
    }

    private BlockedReason resolveBlockedReason(VolunteerRound round,
                                               boolean admitted,
                                               List<AvailableTeacherItem> available,
                                               boolean processed) {
        if (admitted) {
            return BlockedReason.ADMITTED;
        }
        return switch (round.state()) {
            case UNCONFIGURED -> null;
            case NO_ROUND -> BlockedReason.NO_ROUND;
            case NOT_STARTED -> BlockedReason.NOT_OPEN;
            case ENDED -> BlockedReason.ENDED;
            case OPEN -> {
                if (round.roundNo() != null && round.roundNo() >= 2 && available.isEmpty()) {
                    yield BlockedReason.NO_QUOTA;
                }
                if (processed) {
                    yield BlockedReason.PROCESSED;
                }
                yield null;
            }
        };
    }

    private void validateRoundOpen(VolunteerRound round) {
        if (round.state() == RoundState.OPEN) {
            return;
        }
        String message = switch (round.state()) {
            case NOT_STARTED -> round.roundNo() != null && round.roundNo() == 1
                    ? "志愿填报尚未开始"
                    : "补录填报尚未开始";
            case ENDED -> round.roundNo() != null && round.roundNo() == 1
                    ? "志愿填报已截止"
                    : "补录填报已截止";
            default -> "当前不在录取轮次";
        };
        throw new BusinessException(ErrorCode.CONFLICT, message);
    }

    private List<String> normalizeNames(List<String> teacherNames) {
        if (teacherNames == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String value : teacherNames) {
            if (value == null) {
                continue;
            }
            String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
            if (!normalized.isEmpty()) {
                names.add(normalized);
            }
        }
        return names;
    }

    private List<LabInfo> resolveLabs(List<String> names) {
        List<LabInfo> labs = new ArrayList<>(names.size());
        for (String name : names) {
            List<LabInfo> matches = labInfoService.findByTeacherName(name);
            if (matches.size() != 1) {
                throw new BusinessException(
                        ErrorCode.CONFLICT, "导师姓名不存在或存在重名，请联系负责人");
            }
            labs.add(matches.get(0));
        }
        return labs;
    }

    private VolunteerApplicationResponse toApplicationResponse(VolunteerApplication application) {
        ResumeFile resumeFile = resumeFileService.findById(application.getResumeFileId());
        List<VolunteerTargetResponse> targets = targetMapper
                .findTargetsByApplicationId(application.getId());
        return new VolunteerApplicationResponse(
                application.getId(),
                application.getRoundNo(),
                application.getSubmittedAt(),
                Boolean.TRUE.equals(application.getIntraLabAdjust()),
                Boolean.TRUE.equals(application.getCollegeAdjust()),
                application.getResumeFileId(),
                resumeFile == null ? null : resumeFile.getOriginalName(),
                targets);
    }

    private BusinessException invalidSubmit() {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, "提交失败，请重试");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
