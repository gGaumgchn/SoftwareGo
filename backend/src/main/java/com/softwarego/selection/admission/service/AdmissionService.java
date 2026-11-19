package com.softwarego.selection.admission.service;

import com.softwarego.selection.admission.domain.AdmissionRound;
import com.softwarego.selection.admission.domain.AdmissionRoundState;
import com.softwarego.selection.admission.domain.AdmissionTarget;
import com.softwarego.selection.admission.domain.GrantCandidate;
import com.softwarego.selection.admission.dto.GrantOfferRequest;
import com.softwarego.selection.admission.dto.GrantOfferResponse;
import com.softwarego.selection.admission.dto.GrantedOfferItem;
import com.softwarego.selection.admission.dto.ScoreUpdateRequest;
import com.softwarego.selection.admission.dto.ScoreUpdateResponse;
import com.softwarego.selection.admission.dto.TeacherAdmissionSummaryResponse;
import com.softwarego.selection.admission.dto.TeacherApplicationItem;
import com.softwarego.selection.admission.dto.TeacherApplicationListResponse;
import com.softwarego.selection.admission.dto.TieOrderUpdateRequest;
import com.softwarego.selection.admission.dto.TieOrderUpdateResponse;
import com.softwarego.selection.admission.mapper.AdmissionMapper;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.service.LabInfoService;
import com.softwarego.selection.offer.domain.Offer;
import com.softwarego.selection.offer.domain.OfferStatus;
import com.softwarego.selection.offer.mapper.OfferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionService {

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final AdmissionMapper admissionMapper;
    private final AdmissionRoundResolver roundResolver;
    private final LabInfoService labInfoService;
    private final OfferMapper offerMapper;
    private final Clock clock;

    @Transactional
    public TeacherApplicationListResponse getApplications(Long teacherId, Integer roundNo) {
        AdmissionRound round = roundResolver.resolveRequested(roundNo);
        LocalDateTime now = now();
        // 本人本轮已过确认截止的 PENDING 先做惰性校正。
        offerMapper.expirePendingForTeacherAndRound(teacherId, round.roundNo(), now);

        LabInfo lab = labInfoService.findByTeacherId(teacherId);
        boolean canScore = round.state() == AdmissionRoundState.OPEN;
        boolean canGrant = canScore && lab != null;

        List<TeacherApplicationItem> list = admissionMapper.findApplications(teacherId, round.roundNo());
        for (TeacherApplicationItem item : list) {
            // 已产生 offer（含已放弃）的目标不可改分/改顺序。
            item.setScoreEditable(canScore && item.getOfferId() == null);
            item.setTieOrderEditable(item.isScoreEditable() && item.getScore() != null);
        }
        return new TeacherApplicationListResponse(
                round.serverTime(), round.roundNo(), round.state(),
                round.roundStart(), round.roundEnd(), canScore, canGrant, list);
    }

    public TeacherAdmissionSummaryResponse getSummary(Long teacherId, Integer roundNo) {
        AdmissionRound round = roundResolver.resolveRequested(roundNo);
        LabInfo lab = labInfoService.findByTeacherId(teacherId);
        long admittedCount = admissionMapper.countFinalAdmissions(teacherId);
        long pendingCount = admissionMapper.countPendingOffers(teacherId, now());
        Integer plannedQuota = lab == null ? null : lab.getPlannedQuota();
        Long remainingQuota = plannedQuota == null
                ? null
                : plannedQuota - admittedCount - pendingCount;
        boolean canGrant = round.state() == AdmissionRoundState.OPEN && lab != null;
        return new TeacherAdmissionSummaryResponse(
                round.serverTime(), round.roundNo(), round.state(),
                round.roundStart(), round.roundEnd(), lab != null,
                plannedQuota, admittedCount, pendingCount, remainingQuota, canGrant);
    }

    @Transactional
    public ScoreUpdateResponse updateScore(Long teacherId, Long targetId, ScoreUpdateRequest request) {
        AdmissionTarget target = requireTarget(teacherId, targetId);
        BigDecimal score = normalizeScore(request == null ? null : request.score());
        AdmissionRound round = roundResolver.resolveForTarget(target.getRoundNo());
        if (admissionMapper.countOfferByTargetId(targetId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已发放拟录取，分数不可修改");
        }

        LocalDateTime now = now();
        admissionMapper.updateScore(targetId, score, now);
        log.info("录入面试分数: teacherId={}, targetId={}, roundNo={}, score={}",
                teacherId, targetId, round.roundNo(), score);
        return new ScoreUpdateResponse(targetId, score, null, now);
    }

    @Transactional
    public TieOrderUpdateResponse updateTieOrder(Long teacherId, Long targetId,
                                                 TieOrderUpdateRequest request) {
        AdmissionTarget target = requireTarget(teacherId, targetId);
        if (target.getScore() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先录入面试分数");
        }
        AdmissionRound round = roundResolver.resolveForTarget(target.getRoundNo());
        if (admissionMapper.countOfferByTargetId(targetId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已发放拟录取，顺序不可修改");
        }

        Integer tieOrder = request == null ? null : request.tieOrder();
        if (tieOrder == null || tieOrder <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "同分顺序不能重复");
        }
        if (admissionMapper.countTieOrderConflict(
                teacherId, target.getRoundNo(), target.getScore(), tieOrder, targetId) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "同分顺序不能重复");
        }

        LocalDateTime now = now();
        admissionMapper.updateTieOrder(targetId, tieOrder, now);
        log.info("设置同分顺序: teacherId={}, targetId={}, roundNo={}, tieOrder={}",
                teacherId, targetId, round.roundNo(), tieOrder);
        return new TieOrderUpdateResponse(targetId, target.getScore(), tieOrder, now);
    }

    @Transactional
    public GrantOfferResponse grant(Long teacherId, GrantOfferRequest request) {
        BigDecimal cutoffScore = normalizeScore(request == null ? null : request.cutoffScore());
        AdmissionRound round = roundResolver.resolveCurrentForGrant();

        // 锁定导师名额行；同一导师的发放/递补串行化。
        LabInfo lab = labInfoService.findByTeacherIdForUpdate(teacherId);
        if (lab == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先提交实训室信息");
        }

        LocalDateTime now = now();
        offerMapper.expirePendingForTeacherAndRound(teacherId, round.roundNo(), now);
        long admittedCount = admissionMapper.countFinalAdmissions(teacherId);
        long pendingCount = admissionMapper.countPendingOffers(teacherId, now);
        long remaining = (long) lab.getPlannedQuota() - admittedCount - pendingCount;
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "名额已满");
        }

        List<GrantCandidate> candidates = admissionMapper.findGrantCandidates(
                teacherId, round.roundNo(), cutoffScore);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "暂无可发放的达标学生");
        }
        validateTieOrder(candidates);

        List<GrantedOfferItem> granted = new ArrayList<>();
        for (GrantCandidate candidate : candidates) {
            if (granted.size() >= remaining) {
                break;
            }
            Offer offer = new Offer();
            offer.setTeacherId(teacherId);
            offer.setStudentId(candidate.getStudentId());
            offer.setApplicationTargetId(candidate.getTargetId());
            offer.setRoundNo(round.roundNo());
            offer.setStatus(OfferStatus.PENDING);
            offer.setConfirmDeadline(round.roundEnd());
            offer.setGrantedAt(now);
            admissionMapper.insertOffer(offer);

            granted.add(new GrantedOfferItem(
                    offer.getId(),
                    candidate.getStudentId(),
                    candidate.getStaffNo(),
                    candidate.getStudentName(),
                    candidate.getScore(),
                    OfferStatus.PENDING,
                    round.roundEnd()));
        }

        if (granted.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "暂无可发放的达标学生");
        }

        log.info("发放拟录取: teacherId={}, roundNo={}, cutoffScore={}, grantedCount={}",
                teacherId, round.roundNo(), cutoffScore, granted.size());
        return new GrantOfferResponse(
                round.roundNo(), granted.size(), granted,
                remaining - granted.size(), "发放成功");
    }

    /**
     * 同分学生需要导师手动定序；只要达标候选里有多人同分且存在未定序，整批不发放。
     */
    private void validateTieOrder(List<GrantCandidate> candidates) {
        Map<BigDecimal, List<GrantCandidate>> groups = new TreeMap<>();
        for (GrantCandidate candidate : candidates) {
            BigDecimal key = candidate.getScore().stripTrailingZeros();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
        }
        for (List<GrantCandidate> group : groups.values()) {
            if (group.size() > 1 && group.stream().anyMatch(item -> item.getTieOrder() == null)) {
                throw new BusinessException(ErrorCode.CONFLICT, "请先确定同分学生顺序");
            }
        }
    }

    private AdmissionTarget requireTarget(Long teacherId, Long targetId) {
        AdmissionTarget target = admissionMapper.findTarget(targetId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!teacherId.equals(target.getTeacherId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return target;
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(MAX_SCORE) > 0
                || value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "请填写有效分数");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
