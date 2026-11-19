package com.softwarego.selection.stage.service;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import com.softwarego.selection.announcement.service.AnnouncementPublishResult;
import com.softwarego.selection.announcement.service.AnnouncementPublisher;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.common.PageResult;
import com.softwarego.selection.stage.domain.StageChange;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.dto.AnnouncementRetryResponse;
import com.softwarego.selection.stage.dto.StageChangeResponse;
import com.softwarego.selection.stage.dto.StageSaveRequest;
import com.softwarego.selection.stage.dto.StageSaveResponse;
import com.softwarego.selection.stage.mapper.StageChangeMapper;
import com.softwarego.selection.user.domain.User;
import com.softwarego.selection.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StageConfigService {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CHANGE_NO_MAX_LENGTH = 64;

    private final StageConfigTxService stageConfigTxService;
    private final StageChangeMapper stageChangeMapper;
    private final AnnouncementPublisher announcementPublisher;
    private final UserService userService;

    /**
     * 保存阶段并在事务提交后独立发布公告。
     * changeNo 重复时直接返回已有结果，不再保存阶段、不再发布。
     */
    public StageSaveResponse save(StageSaveRequest request, Long operatorId) {
        StageSaveCommand command = validateAndBuild(request);
        StageSaveOutcome outcome = stageConfigTxService.save(command, operatorId);
        StageChange change = outcome.change();

        if (!outcome.newlySaved()) {
            log.info("阶段保存幂等命中: operatorId={}, changeNo={}", operatorId, command.changeNo());
            return toExistingResponse(change);
        }

        if (!command.publishAnnouncement()) {
            log.info("阶段保存成功: operatorId={}, changeNo={}, announcement=NONE",
                    operatorId, command.changeNo());
            return new StageSaveResponse(command.changeNo(), true, AnnouncementStatus.NONE, null, "保存成功");
        }

        try {
            AnnouncementPublishResult result = announcementPublisher.publish(command.changeNo());
            log.info("阶段保存成功: operatorId={}, changeNo={}, announcement=PUBLISHED, announcementId={}",
                    operatorId, command.changeNo(), result.announcementId());
            return new StageSaveResponse(
                    command.changeNo(), true, AnnouncementStatus.PUBLISHED,
                    result.announcementId(), "保存成功，公告已发布");
        } catch (RuntimeException exception) {
            log.error("阶段已保存，公告发布失败: changeNo={}", command.changeNo(), exception);
            markFailedQuietly(command.changeNo());
            return new StageSaveResponse(
                    command.changeNo(), true, AnnouncementStatus.FAILED, null,
                    "阶段已保存，公告发布失败，请重试");
        }
    }

    /**
     * 只重试公告，不更新 stage、不新增 stage_change。
     */
    public AnnouncementRetryResponse retryAnnouncement(String changeNo) {
        validateChangeNo(changeNo);
        try {
            AnnouncementPublishResult result = announcementPublisher.publish(changeNo);
            return new AnnouncementRetryResponse(
                    changeNo, AnnouncementStatus.PUBLISHED, result.announcementId(), "公告已发布");
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("公告重试发布失败: changeNo={}", changeNo, exception);
            markFailedQuietly(changeNo);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "公告发布失败，请重试");
        }
    }

    public PageResult<StageChangeResponse> listChanges(AnnouncementStatus status, int page, int pageSize) {
        validatePage(page, pageSize);
        long offset = (long) (page - 1) * pageSize;
        List<StageChange> changes = stageChangeMapper.findPage(status, offset, pageSize);
        long total = stageChangeMapper.count(status);

        List<StageChangeResponse> list = changes.stream()
                .map(this::toStageChangeResponse)
                .toList();
        return PageResult.of(list, total, page, pageSize);
    }

    private StageChangeResponse toStageChangeResponse(StageChange change) {
        User operator = userService.findById(change.getOperatorId());
        String operatorName = operator == null ? null : operator.getName();
        return new StageChangeResponse(
                change.getChangeNo(),
                change.getOperatorId(),
                operatorName,
                Boolean.TRUE.equals(change.getPublishRequested()),
                change.getAnnouncementStatus(),
                change.getDraftTitle(),
                change.getAnnouncementId(),
                change.getCreatedAt(),
                change.getUpdatedAt());
    }

    private StageSaveResponse toExistingResponse(StageChange change) {
        AnnouncementStatus status = change.getAnnouncementStatus();
        String message = switch (status) {
            case PUBLISHED -> "保存成功，公告已发布";
            case FAILED -> "阶段已保存，公告发布失败，请重试";
            case PENDING -> "阶段已保存，公告待发布";
            case NONE -> "保存成功";
        };
        return new StageSaveResponse(change.getChangeNo(), true, status, change.getAnnouncementId(), message);
    }

    private void markFailedQuietly(String changeNo) {
        try {
            announcementPublisher.markFailed(changeNo);
        } catch (RuntimeException exception) {
            log.error("标记公告发布失败时出错: changeNo={}", changeNo, exception);
        }
    }

    private StageSaveCommand validateAndBuild(StageSaveRequest request) {
        if (request == null) {
            throw invalidRequest();
        }
        validateChangeNo(request.changeNo());

        if (request.publishAnnouncement() == null) {
            throw invalidRequest();
        }
        if (request.stages() == null || request.stages().size() != StageCode.values().length) {
            throw invalidRequest();
        }

        Map<StageCode, StageSaveRequest.StageSaveItem> itemsByCode = new EnumMap<>(StageCode.class);
        for (StageSaveRequest.StageSaveItem item : request.stages()) {
            if (item == null || isBlank(item.stageCode())) {
                throw invalidRequest();
            }
            StageCode stageCode = StageCode.from(item.stageCode());
            if (stageCode == null || itemsByCode.put(stageCode, item) != null) {
                throw invalidRequest();
            }
        }
        if (itemsByCode.size() != StageCode.values().length) {
            throw invalidRequest();
        }

        List<StageSaveCommand.StageTimeCommand> timeCommands = new ArrayList<>(StageCode.values().length);
        for (StageCode stageCode : StageCode.values()) {
            StageSaveRequest.StageSaveItem item = itemsByCode.get(stageCode);
            if (item.startAt() == null || item.endAt() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请填写完整的阶段时间");
            }
            LocalDateTime startAt = item.startAt().truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime endAt = item.endAt().truncatedTo(ChronoUnit.MINUTES);
            if (!startAt.isBefore(endAt)) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "阶段结束时间必须晚于开始时间");
            }
            timeCommands.add(new StageSaveCommand.StageTimeCommand(stageCode, startAt, endAt));
        }

        for (int index = 1; index < timeCommands.size(); index += 1) {
            LocalDateTime previousEnd = timeCommands.get(index - 1).endAt();
            LocalDateTime currentStart = timeCommands.get(index).startAt();
            if (previousEnd.isAfter(currentStart)) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER, "阶段时间必须按流程顺序设置且不能重叠");
            }
        }

        boolean publishAnnouncement = request.publishAnnouncement();
        String title = null;
        String content = null;
        if (publishAnnouncement) {
            StageSaveRequest.AnnouncementDraftRequest draft = request.announcement();
            if (draft == null || isBlank(draft.title()) || isBlank(draft.content())) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "公告内容不能为空");
            }
            title = draft.title().trim();
            content = draft.content().trim();
            if (title.length() > TITLE_MAX_LENGTH) {
                throw invalidRequest();
            }
        }

        return new StageSaveCommand(
                request.changeNo(),
                List.copyOf(timeCommands),
                publishAnnouncement,
                title,
                content);
    }

    private void validateChangeNo(String changeNo) {
        if (isBlank(changeNo) || changeNo.length() > CHANGE_NO_MAX_LENGTH) {
            throw invalidRequest();
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "分页参数不合法");
        }
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, "提交失败，请重试");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
