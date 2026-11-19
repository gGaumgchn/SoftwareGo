package com.softwarego.selection.volunteer;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.service.StageService;
import com.softwarego.selection.volunteer.domain.RoundState;
import com.softwarego.selection.volunteer.domain.VolunteerRound;
import com.softwarego.selection.volunteer.service.VolunteerRoundResolver;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用固定 Clock 验证轮次解析：开放前/开放中/空档/全部结束/未配置。
 * 空档语义按现有实现：下一轮为 NOT_STARTED（见 B4 已记录的业务歧义）。
 */
class VolunteerRoundResolverTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final StageService stageService = mock(StageService.class);
    private VolunteerRoundResolver roundResolver;

    @Test
    void beforeAllRoundsIsNotStartedOnRound1() {
        withStages(rounds(
                NOW.plusHours(1), NOW.plusHours(2),
                NOW.plusHours(3), NOW.plusHours(4),
                NOW.plusHours(5), NOW.plusHours(6)));

        VolunteerRound round = roundResolver.resolve();

        assertThat(round.state()).isEqualTo(RoundState.NOT_STARTED);
        assertThat(round.roundNo()).isEqualTo(1);
    }

    @Test
    void round1Open() {
        withStages(rounds(
                NOW.minusHours(1), NOW.plusHours(1),
                NOW.plusHours(3), NOW.plusHours(4),
                NOW.plusHours(5), NOW.plusHours(6)));

        VolunteerRound round = roundResolver.resolve();

        assertThat(round.state()).isEqualTo(RoundState.OPEN);
        assertThat(round.roundNo()).isEqualTo(1);
    }

    @Test
    void gapReturnsNextRoundNotStarted() {
        withStages(rounds(
                NOW.minusHours(2), NOW.minusHours(1),
                NOW.plusHours(1), NOW.plusHours(2),
                NOW.plusHours(3), NOW.plusHours(4)));

        VolunteerRound round = roundResolver.resolve();

        // 当前实现：空档返回下一轮 NOT_STARTED；B4 文档已记录该歧义。
        assertThat(round.state()).isEqualTo(RoundState.NOT_STARTED);
        assertThat(round.roundNo()).isEqualTo(2);
    }

    @Test
    void round2Open() {
        withStages(rounds(
                NOW.minusHours(4), NOW.minusHours(3),
                NOW.minusHours(1), NOW.plusHours(1),
                NOW.plusHours(3), NOW.plusHours(4)));

        VolunteerRound round = roundResolver.resolve();

        assertThat(round.state()).isEqualTo(RoundState.OPEN);
        assertThat(round.roundNo()).isEqualTo(2);
    }

    @Test
    void afterAllRoundsIsEndedOnRound3() {
        withStages(rounds(
                NOW.minusHours(6), NOW.minusHours(5),
                NOW.minusHours(4), NOW.minusHours(3),
                NOW.minusHours(2), NOW.minusHours(1)));

        VolunteerRound round = roundResolver.resolve();

        assertThat(round.state()).isEqualTo(RoundState.ENDED);
        assertThat(round.roundNo()).isEqualTo(3);
    }

    @Test
    void missingRoundTimeIsUnconfigured() {
        withStages(List.of(
                stage(StageCode.ROUND_1, NOW.minusHours(2), NOW.minusHours(1)),
                stage(StageCode.ROUND_2, null, null),
                stage(StageCode.ROUND_3, NOW.plusHours(3), NOW.plusHours(4))));

        VolunteerRound round = roundResolver.resolve();

        assertThat(round.state()).isEqualTo(RoundState.UNCONFIGURED);
        assertThat(round.roundNo()).isNull();
    }

    private void withStages(List<Stage> stages) {
        when(stageService.loadFixedStages()).thenReturn(stages);
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        roundResolver = new VolunteerRoundResolver(stageService, clock);
    }

    private List<Stage> rounds(LocalDateTime start1, LocalDateTime end1,
                               LocalDateTime start2, LocalDateTime end2,
                               LocalDateTime start3, LocalDateTime end3) {
        return List.of(
                stage(StageCode.ROUND_1, start1, end1),
                stage(StageCode.ROUND_2, start2, end2),
                stage(StageCode.ROUND_3, start3, end3));
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
}
