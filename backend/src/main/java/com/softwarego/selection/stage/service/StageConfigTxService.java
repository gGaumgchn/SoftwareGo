package com.softwarego.selection.stage.service;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.config.StageProperties;
import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageChange;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.mapper.StageChangeMapper;
import com.softwarego.selection.stage.mapper.StageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 阶段保存事务：锁定 7 行 stage，检查 changeNo 幂等，按开关校验“已截止阶段不得延长”，
 * 更新 stage 并写入 stage_change。公告发布不在本事务内，由调用方在提交后独立执行。
 */
@Service
@RequiredArgsConstructor
public class StageConfigTxService {

    private final StageMapper stageMapper;
    private final StageChangeMapper stageChangeMapper;
    private final StageProperties stageProperties;
    private final Clock clock;

    @Transactional
    public StageSaveOutcome save(StageSaveCommand command, Long operatorId) {
        // 锁定 7 行 stage，使并发保存串行化，保证 changeNo 幂等检查与时间校验看到一致旧值。
        List<Stage> lockedStages = stageMapper.findAllForUpdate();
        Map<StageCode, Stage> oldByCode = new EnumMap<>(StageCode.class);
        for (Stage stage : lockedStages) {
            oldByCode.put(stage.getStageCode(), stage);
        }

        StageChange existing = stageChangeMapper.findByChangeNo(command.changeNo());
        if (existing != null) {
            return new StageSaveOutcome(existing, false);
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        if (!stageProperties.isAllowExtendEnded()) {
            validateNotExtendedAfterEnded(oldByCode, command.stages(), now);
        }

        for (StageSaveCommand.StageTimeCommand item : command.stages()) {
            stageMapper.updateTimes(item.stageCode(), item.startAt(), item.endAt());
        }

        StageChange change = new StageChange();
        change.setChangeNo(command.changeNo());
        change.setOperatorId(operatorId);
        change.setPublishRequested(command.publishAnnouncement());
        change.setAnnouncementStatus(command.publishAnnouncement()
                ? AnnouncementStatus.PENDING
                : AnnouncementStatus.NONE);
        if (command.publishAnnouncement()) {
            change.setDraftTitle(command.announcementTitle());
            change.setDraftContent(command.announcementContent());
        }
        stageChangeMapper.insert(change);

        return new StageSaveOutcome(change, true);
    }

    /**
     * D3：旧 end_at 已到达/已过（旧 end <= 服务端当前时间）时，新 end_at 不得晚于旧值。
     * 只禁止“延长”，允许保持或缩短；阶段调整不重新开放已截止窗口。
     * 演示开关 app.stage.allow-extend-ended=true 时跳过本校验。
     */
    private void validateNotExtendedAfterEnded(Map<StageCode, Stage> oldByCode,
                                               List<StageSaveCommand.StageTimeCommand> newStages,
                                               LocalDateTime now) {
        for (StageSaveCommand.StageTimeCommand item : newStages) {
            Stage old = oldByCode.get(item.stageCode());
            if (old == null || old.getEndAt() == null) {
                continue;
            }
            if (!old.getEndAt().isAfter(now) && item.endAt().isAfter(old.getEndAt())) {
                throw new BusinessException(ErrorCode.CONFLICT, "阶段已截止，不能延长");
            }
        }
    }
}
