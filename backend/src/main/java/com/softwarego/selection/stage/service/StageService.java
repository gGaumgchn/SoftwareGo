package com.softwarego.selection.stage.service;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.dto.CurrentStageResponse;
import com.softwarego.selection.stage.dto.ManagerStageResponse;
import com.softwarego.selection.stage.mapper.StageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StageService {

    private final StageMapper stageMapper;
    private final Clock clock;

    public CurrentStageResponse getCurrentStage() {
        LocalDateTime now = now();
        List<Stage> stages = loadFixedStages();
        StageTimeline timeline = StageTimeline.of(stages, now);

        return new CurrentStageResponse(
                now,
                timeline.status(),
                timeline.configured(),
                toSummary(timeline.current()),
                toSummary(timeline.next()),
                stages.stream()
                        .map(stage -> new CurrentStageResponse.StageStateItem(
                                stage.getStageCode(),
                                stage.getStageName(),
                                stage.getSortNo(),
                                stage.getStartAt(),
                                stage.getEndAt(),
                                timeline.stateOf(stage)))
                        .toList());
    }

    public ManagerStageResponse getManagerStages() {
        LocalDateTime now = now();
        List<Stage> stages = loadFixedStages();
        StageTimeline timeline = StageTimeline.of(stages, now);
        Stage current = timeline.current();

        return new ManagerStageResponse(
                now,
                timeline.configured(),
                current == null ? null : current.getStageCode(),
                stages.stream()
                        .map(stage -> new ManagerStageResponse.StageItem(
                                stage.getStageCode(),
                                stage.getStageName(),
                                stage.getSortNo(),
                                stage.getStartAt(),
                                stage.getEndAt()))
                        .toList());
    }

    /**
     * 固定 7 行：以 StageCode 枚举顺序为基准，合并数据库中的名称、顺序与时间。
     * 即使数据库缺行，也返回完整的 7 个阶段，避免前端步骤条缺项。
     */
    public List<Stage> loadFixedStages() {
        Map<StageCode, Stage> rowsByCode = new EnumMap<>(StageCode.class);
        for (Stage row : stageMapper.findAll()) {
            if (row.getStageCode() != null) {
                rowsByCode.put(row.getStageCode(), row);
            }
        }

        List<Stage> stages = new ArrayList<>(StageCode.values().length);
        for (StageCode stageCode : StageCode.values()) {
            Stage row = rowsByCode.get(stageCode);
            if (row != null) {
                stages.add(row);
                continue;
            }
            Stage missing = new Stage();
            missing.setStageCode(stageCode);
            missing.setStageName(stageCode.getStageName());
            missing.setSortNo(stageCode.getSortNo());
            stages.add(missing);
        }
        return stages;
    }

    private CurrentStageResponse.StageSummary toSummary(Stage stage) {
        if (stage == null) {
            return null;
        }
        return new CurrentStageResponse.StageSummary(
                stage.getStageCode(),
                stage.getStageName(),
                stage.getSortNo(),
                stage.getStartAt(),
                stage.getEndAt());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
