package com.softwarego.selection.stage;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.domain.StageState;
import com.softwarego.selection.stage.domain.StageStatus;
import com.softwarego.selection.stage.service.StageTimeline;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 固定时间点验证阶段时间线：开放前/开放中/空档/全部结束/半开区间/未配置。
 * 不依赖系统时钟，直接用传入的 now 控制边界。
 */
class StageTimelineTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);

    @Test
    void beforeAllStages() {
        List<Stage> stages = sequentialFrom(NOW.plusDays(1));
        StageTimeline timeline = StageTimeline.of(stages, NOW);

        assertThat(timeline.configured()).isTrue();
        assertThat(timeline.status()).isEqualTo(StageStatus.BETWEEN);
        assertThat(timeline.current()).isNull();
        assertThat(timeline.next().getStageCode()).isEqualTo(StageCode.BRIEFING);
        assertThat(timeline.states().values())
                .containsOnly(StageState.NOT_STARTED);
    }

    @Test
    void ongoingStage() {
        List<Stage> stages = intervals(new String[][]{
                {"BRIEFING", "-4h", "-3h"},
                {"OPEN_DAY", "-2h", "+2h"},
                {"ROUND_1", "+3h", "+4h"},
                {"ROUND_2", "+5h", "+6h"},
                {"ROUND_3", "+7h", "+8h"},
                {"ADJUSTMENT", "+9h", "+10h"},
                {"ENROLLMENT", "+11h", "+12h"}
        });
        StageTimeline timeline = StageTimeline.of(stages, NOW);

        assertThat(timeline.status()).isEqualTo(StageStatus.ONGOING);
        assertThat(timeline.current().getStageCode()).isEqualTo(StageCode.OPEN_DAY);
        assertThat(timeline.next()).isNull();
        assertThat(timeline.stateOf(stages.get(0))).isEqualTo(StageState.FINISHED);
        assertThat(timeline.stateOf(stages.get(2))).isEqualTo(StageState.NOT_STARTED);
    }

    @Test
    void gapBetweenStages() {
        List<Stage> stages = intervals(new String[][]{
                {"BRIEFING", "-4h", "-2h"},
                {"OPEN_DAY", "+2h", "+4h"},
                {"ROUND_1", "+5h", "+6h"},
                {"ROUND_2", "+7h", "+8h"},
                {"ROUND_3", "+9h", "+10h"},
                {"ADJUSTMENT", "+11h", "+12h"},
                {"ENROLLMENT", "+13h", "+14h"}
        });
        StageTimeline timeline = StageTimeline.of(stages, NOW);

        assertThat(timeline.status()).isEqualTo(StageStatus.BETWEEN);
        assertThat(timeline.current()).isNull();
        assertThat(timeline.next().getStageCode()).isEqualTo(StageCode.OPEN_DAY);
        assertThat(timeline.stateOf(stages.get(0))).isEqualTo(StageState.FINISHED);
        assertThat(timeline.stateOf(stages.get(1))).isEqualTo(StageState.NOT_STARTED);
    }

    @Test
    void allStagesEnded() {
        List<Stage> stages = intervals(new String[][]{
                {"BRIEFING", "-14h", "-13h"},
                {"OPEN_DAY", "-12h", "-11h"},
                {"ROUND_1", "-10h", "-9h"},
                {"ROUND_2", "-8h", "-7h"},
                {"ROUND_3", "-6h", "-5h"},
                {"ADJUSTMENT", "-4h", "-3h"},
                {"ENROLLMENT", "-2h", "-1h"}
        });
        StageTimeline timeline = StageTimeline.of(stages, NOW);

        assertThat(timeline.status()).isEqualTo(StageStatus.ENDED);
        assertThat(timeline.current()).isNull();
        assertThat(timeline.next()).isNull();
        assertThat(timeline.states().values()).containsOnly(StageState.FINISHED);
    }

    @Test
    void boundaryAtEndIsFinishedAndNextStartIsOngoing() {
        List<Stage> stages = intervals(new String[][]{
                {"BRIEFING", "-1h", "0h"},
                {"OPEN_DAY", "0h", "+1h"},
                {"ROUND_1", "+2h", "+3h"},
                {"ROUND_2", "+4h", "+5h"},
                {"ROUND_3", "+6h", "+7h"},
                {"ADJUSTMENT", "+8h", "+9h"},
                {"ENROLLMENT", "+10h", "+11h"}
        });
        StageTimeline timeline = StageTimeline.of(stages, NOW);

        // 半开区间 [startAt, endAt)：endAt 恰好等于 now 时算已结束。
        assertThat(timeline.stateOf(stages.get(0))).isEqualTo(StageState.FINISHED);
        assertThat(timeline.stateOf(stages.get(1))).isEqualTo(StageState.ONGOING);
        assertThat(timeline.status()).isEqualTo(StageStatus.ONGOING);
        assertThat(timeline.current().getStageCode()).isEqualTo(StageCode.OPEN_DAY);
    }

    @Test
    void unconfiguredWhenAnyStageMissingTimes() {
        List<Stage> stages = sequentialFrom(NOW.plusDays(1));
        stages.get(3).setStartAt(null);
        stages.get(3).setEndAt(null);

        StageTimeline timeline = StageTimeline.of(stages, NOW);

        assertThat(timeline.configured()).isFalse();
        assertThat(timeline.status()).isEqualTo(StageStatus.UNCONFIGURED);
        assertThat(timeline.current()).isNull();
        assertThat(timeline.next()).isNull();
        assertThat(timeline.stateOf(stages.get(3))).isEqualTo(StageState.UNCONFIGURED);
    }

    private List<Stage> sequentialFrom(LocalDateTime firstStart) {
        List<Stage> stages = new ArrayList<>();
        LocalDateTime cursor = firstStart;
        for (StageCode code : StageCode.values()) {
            stages.add(stage(code, cursor, cursor.plusHours(1)));
            cursor = cursor.plusHours(2);
        }
        return stages;
    }

    private List<Stage> intervals(String[][] specs) {
        List<Stage> stages = new ArrayList<>();
        for (int index = 0; index < StageCode.values().length; index += 1) {
            StageCode code = StageCode.values()[index];
            stages.add(stage(code, offset(specs[index][1]), offset(specs[index][2])));
        }
        return stages;
    }

    private Stage stage(StageCode code, LocalDateTime startAt, LocalDateTime endAt) {
        Stage stage = new Stage();
        stage.setStageCode(code);
        stage.setStageName(code.getStageName());
        stage.setSortNo(code.getSortNo());
        stage.setStartAt(startAt);
        stage.setEndAt(endAt);
        return stage;
    }

    private LocalDateTime offset(String value) {
        if ("0h".equals(value)) {
            return NOW;
        }
        int hours = Integer.parseInt(value.substring(0, value.length() - 1));
        return NOW.plusHours(hours);
    }
}
