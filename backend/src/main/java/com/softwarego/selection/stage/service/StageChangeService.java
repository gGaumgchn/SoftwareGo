package com.softwarego.selection.stage.service;

import com.softwarego.selection.stage.domain.StageChange;
import com.softwarego.selection.stage.mapper.StageChangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * stage_change 的跨模块访问入口。公告发布服务通过本服务读写阶段变更记录，
 * 不直接注入 stage 模块的 Mapper。
 */
@Service
@RequiredArgsConstructor
public class StageChangeService {

    private final StageChangeMapper stageChangeMapper;

    public StageChange lockByChangeNo(String changeNo) {
        return stageChangeMapper.findByChangeNoForUpdate(changeNo);
    }

    public StageChange findByChangeNo(String changeNo) {
        return stageChangeMapper.findByChangeNo(changeNo);
    }

    public void markPublished(Long id, Long announcementId) {
        stageChangeMapper.markPublished(id, announcementId);
    }

    public void markFailed(Long id) {
        stageChangeMapper.markFailed(id);
    }
}
