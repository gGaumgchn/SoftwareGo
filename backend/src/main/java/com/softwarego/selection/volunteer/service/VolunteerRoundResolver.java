package com.softwarego.selection.volunteer.service;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.service.StageService;
import com.softwarego.selection.volunteer.domain.RoundState;
import com.softwarego.selection.volunteer.domain.VolunteerRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 根据 stage 表解析当前/下一可填报轮次，不接受前端传入 roundNo。
 */
@Service
@RequiredArgsConstructor
public class VolunteerRoundResolver {

    private final StageService stageService;
    private final Clock clock;

    public VolunteerRound resolve() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        List<Stage> stages = stageService.loadFixedStages();
        Stage round1 = findStage(stages, StageCode.ROUND_1);
        Stage round2 = findStage(stages, StageCode.ROUND_2);
        Stage round3 = findStage(stages, StageCode.ROUND_3);

        if (!hasTimes(round1) || !hasTimes(round2) || !hasTimes(round3)) {
            return new VolunteerRound(now, null, RoundState.UNCONFIGURED, null, null);
        }

        if (isOpen(round1, now)) {
            return round(now, 1, RoundState.OPEN, round1);
        }
        if (isOpen(round2, now)) {
            return round(now, 2, RoundState.OPEN, round2);
        }
        if (isOpen(round3, now)) {
            return round(now, 3, RoundState.OPEN, round3);
        }

        if (now.isBefore(round1.getStartAt())) {
            return round(now, 1, RoundState.NOT_STARTED, round1);
        }
        if (now.isBefore(round2.getStartAt())) {
            return round(now, 2, RoundState.NOT_STARTED, round2);
        }
        if (now.isBefore(round3.getStartAt())) {
            return round(now, 3, RoundState.NOT_STARTED, round3);
        }
        return round(now, 3, RoundState.ENDED, round3);
    }

    private Stage findStage(List<Stage> stages, StageCode stageCode) {
        return stages.stream()
                .filter(stage -> stage.getStageCode() == stageCode)
                .findFirst()
                .orElse(null);
    }

    private boolean hasTimes(Stage stage) {
        return stage != null && stage.getStartAt() != null && stage.getEndAt() != null;
    }

    private boolean isOpen(Stage stage, LocalDateTime now) {
        return hasTimes(stage)
                && !now.isBefore(stage.getStartAt())
                && now.isBefore(stage.getEndAt());
    }

    private VolunteerRound round(LocalDateTime now, int roundNo, RoundState state, Stage stage) {
        return new VolunteerRound(
                now, roundNo, state, stage.getStartAt(), stage.getEndAt());
    }
}
