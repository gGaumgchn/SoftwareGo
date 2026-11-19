package com.softwarego.selection.admission.service;

import com.softwarego.selection.admission.domain.AdmissionRound;
import com.softwarego.selection.admission.domain.AdmissionRoundState;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 导师录取页面的轮次解析：支持指定 roundNo，也支持“不传使用当前开放轮次”。
 */
@Service
@RequiredArgsConstructor
public class AdmissionRoundResolver {

    private final StageService stageService;
    private final Clock clock;

    public AdmissionRound resolveRequested(Integer roundNo) {
        LocalDateTime now = now();
        if (roundNo == null) {
            return currentOpen(now);
        }
        return resolve(roundNo, now);
    }

    /**
     * 评分/排序必须仍处于目标轮次开放期内。
     */
    public AdmissionRound resolveForTarget(Integer roundNo) {
        LocalDateTime now = now();
        AdmissionRound round = roundNo == null ? currentOpen(now) : resolve(roundNo, now);
        if (round.state() != AdmissionRoundState.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "本轮已结束");
        }
        return round;
    }

    /**
     * 发放要求当前存在开放轮次；全部轮次结束后提示发放已截止。
     */
    public AdmissionRound resolveCurrentForGrant() {
        LocalDateTime now = now();
        List<Stage> stages = stageService.loadFixedStages();
        for (int roundNo = 1; roundNo <= 3; roundNo += 1) {
            Stage stage = findStage(stages, roundNo);
            if (isOpen(stage, now)) {
                return new AdmissionRound(now, roundNo, AdmissionRoundState.OPEN,
                        stage.getStartAt(), stage.getEndAt());
            }
        }

        Stage round3 = findStage(stages, 3);
        if (hasTimes(round3) && !now.isBefore(round3.getEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "本轮拟录取发放已截止");
        }
        throw new BusinessException(ErrorCode.CONFLICT, "当前不在录取轮次");
    }

    private AdmissionRound currentOpen(LocalDateTime now) {
        List<Stage> stages = stageService.loadFixedStages();
        for (int roundNo = 1; roundNo <= 3; roundNo += 1) {
            Stage stage = findStage(stages, roundNo);
            if (isOpen(stage, now)) {
                return new AdmissionRound(now, roundNo, AdmissionRoundState.OPEN,
                        stage.getStartAt(), stage.getEndAt());
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "当前不在录取轮次");
    }

    private AdmissionRound resolve(int roundNo, LocalDateTime now) {
        validateRoundNo(roundNo);
        Stage stage = findStage(stageService.loadFixedStages(), roundNo);
        if (!hasTimes(stage)) {
            return new AdmissionRound(now, roundNo, AdmissionRoundState.NO_ROUND, null, null);
        }
        AdmissionRoundState state;
        if (now.isBefore(stage.getStartAt())) {
            state = AdmissionRoundState.NOT_STARTED;
        } else if (now.isBefore(stage.getEndAt())) {
            state = AdmissionRoundState.OPEN;
        } else {
            state = AdmissionRoundState.ENDED;
        }
        return new AdmissionRound(now, roundNo, state, stage.getStartAt(), stage.getEndAt());
    }

    private void validateRoundNo(int roundNo) {
        if (roundNo < 1 || roundNo > 3) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "roundNo 非法");
        }
    }

    private Stage findStage(List<Stage> stages, int roundNo) {
        StageCode code = StageCode.valueOf("ROUND_" + roundNo);
        return stages.stream()
                .filter(stage -> stage.getStageCode() == code)
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
