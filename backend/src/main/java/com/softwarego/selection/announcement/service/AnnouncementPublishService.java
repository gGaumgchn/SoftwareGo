package com.softwarego.selection.announcement.service;

import com.softwarego.selection.announcement.domain.Announcement;
import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.stage.domain.StageChange;
import com.softwarego.selection.stage.service.StageChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 公告发布事务。独立于阶段保存事务：阶段先提交生效，公告失败不回滚阶段。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementPublishService implements AnnouncementPublisher {

    private final StageChangeService stageChangeService;
    private final AnnouncementService announcementService;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnnouncementPublishResult publish(String changeNo) {
        StageChange change = stageChangeService.lockByChangeNo(changeNo);
        if (change == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(change.getPublishRequested())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该次保存未请求发布公告");
        }
        if (change.getAnnouncementStatus() == AnnouncementStatus.PUBLISHED) {
            return new AnnouncementPublishResult(
                    changeNo, AnnouncementStatus.PUBLISHED, change.getAnnouncementId());
        }
        if (isBlank(change.getDraftTitle()) || isBlank(change.getDraftContent())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "公告内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Announcement announcement = announcementService.create(
                change.getDraftTitle(), change.getDraftContent(), change.getOperatorId(), now);
        stageChangeService.markPublished(change.getId(), announcement.getId());

        log.info("公告发布成功: changeNo={}, announcementId={}", changeNo, announcement.getId());
        return new AnnouncementPublishResult(
                changeNo, AnnouncementStatus.PUBLISHED, announcement.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String changeNo) {
        StageChange change = stageChangeService.findByChangeNo(changeNo);
        if (change == null || change.getAnnouncementStatus() == AnnouncementStatus.PUBLISHED) {
            return;
        }
        stageChangeService.markFailed(change.getId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
