package com.softwarego.selection.offer.service;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.service.LabInfoService;
import com.softwarego.selection.lab.service.QuotaService;
import com.softwarego.selection.offer.domain.AbandonReason;
import com.softwarego.selection.offer.domain.AdmissionType;
import com.softwarego.selection.offer.domain.FinalAdmission;
import com.softwarego.selection.offer.domain.Offer;
import com.softwarego.selection.offer.domain.OfferStatus;
import com.softwarego.selection.offer.dto.AdmissionResponse;
import com.softwarego.selection.offer.dto.AdmissionResultResponse;
import com.softwarego.selection.offer.dto.ConfirmOfferResponse;
import com.softwarego.selection.offer.dto.OfferItemResponse;
import com.softwarego.selection.offer.dto.OfferListResponse;
import com.softwarego.selection.offer.mapper.FinalAdmissionMapper;
import com.softwarego.selection.offer.mapper.OfferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final OfferMapper offerMapper;
    private final FinalAdmissionMapper finalAdmissionMapper;
    private final LabInfoService labInfoService;
    private final QuotaService quotaService;
    private final Clock clock;

    @Transactional
    public OfferListResponse getOffers(Long studentId) {
        LocalDateTime now = now();
        offerMapper.expirePendingForStudent(studentId, now);
        FinalAdmission finalAdmission = finalAdmissionMapper.findByStudentId(studentId);

        List<OfferItemResponse> list = offerMapper.findByStudentId(studentId);
        for (OfferItemResponse item : list) {
            item.setCanConfirm(item.getStatus() == OfferStatus.PENDING
                    && now.isBefore(item.getConfirmDeadline())
                    && finalAdmission == null);
        }
        return new OfferListResponse(now, list);
    }

    public AdmissionResultResponse getAdmission(Long studentId) {
        FinalAdmission finalAdmission = finalAdmissionMapper.findByStudentId(studentId);
        if (finalAdmission == null) {
            return new AdmissionResultResponse(false, null);
        }
        LabInfo lab = labInfoService.findByTeacherId(finalAdmission.getTeacherId());
        return new AdmissionResultResponse(true, toAdmissionResponse(finalAdmission, lab));
    }

    @Transactional(noRollbackFor = OfferExpiredException.class)
    public ConfirmOfferResponse confirm(Long studentId, Long offerId) {
        try {
            return doConfirm(studentId, offerId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("确认拟录取失败: studentId={}, offerId={}", studentId, offerId, exception);
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR, "确认失败，请刷新查看结果后重试");
        }
    }

    @Transactional
    public void expireAllPendingOffers() {
        int count = offerMapper.expireAllPending(now());
        if (count > 0) {
            log.info("超时自动放弃拟录取: count={}", count);
        }
    }

    /**
     * 线下统筹调剂结果导入。仅供脚本/测试调用，不暴露 HTTP 接口。
     * 事务内锁定导师名额，校验无最终录取、无遗留 PENDING/CONFIRMED offer、名额充足。
     */
    @Transactional
    public FinalAdmission importAdjustment(Long studentId, Long teacherId, AdmissionType admissionType) {
        if (admissionType != AdmissionType.ADJUST_INTRA_LAB
                && admissionType != AdmissionType.ADJUST_COLLEGE) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "调剂类型不合法");
        }

        LabInfo lab = labInfoService.findByTeacherIdForUpdate(teacherId);
        if (lab == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "导师实训室信息不存在");
        }
        if (finalAdmissionMapper.findByStudentId(studentId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "学生已有最终录取");
        }

        LocalDateTime now = now();
        // 遗留 PENDING 先按超时处理，未截止的 PENDING/CONFIRMED 仍会阻止导入。
        offerMapper.expirePendingForStudent(studentId, now);
        if (offerMapper.countBlockingOffers(studentId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "学生存在未处理拟录取");
        }

        long occupied = quotaService.occupied(teacherId);
        if (occupied + 1 > lab.getPlannedQuota()) {
            throw new BusinessException(ErrorCode.CONFLICT, "导师名额不足");
        }

        FinalAdmission finalAdmission = new FinalAdmission();
        finalAdmission.setStudentId(studentId);
        finalAdmission.setTeacherId(teacherId);
        finalAdmission.setRoundNo(null);
        finalAdmission.setAdmissionType(admissionType);
        finalAdmission.setSourceOfferId(null);
        finalAdmission.setAdmittedAt(now);
        try {
            finalAdmissionMapper.insert(finalAdmission);
        } catch (DuplicateKeyException exception) {
            // uk_fa_student 兜底并发/重复导入。
            throw new BusinessException(ErrorCode.CONFLICT, "学生已有最终录取");
        }
        log.info("导入调剂录取: studentId={}, teacherId={}, admissionType={}",
                studentId, teacherId, admissionType);
        return finalAdmission;
    }

    private ConfirmOfferResponse doConfirm(Long studentId, Long offerId) {
        LocalDateTime now = now();
        Offer offer = offerMapper.findById(offerId);
        if (offer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!studentId.equals(offer.getStudentId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (offer.getStatus() == OfferStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "已确认录取，不可更改");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "确认失败，请刷新查看结果后重试");
        }

        // 锁定该学生全部 PENDING，防止并发确认两个 offer 产生两个结果。
        List<Offer> pending = offerMapper.findPendingByStudentForUpdate(studentId);
        Offer target = pending.stream()
                .filter(candidate -> candidate.getId().equals(offerId))
                .findFirst()
                .orElse(null);

        if (target == null) {
            Offer current = offerMapper.findById(offerId);
            if (current == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            if (current.getStatus() == OfferStatus.CONFIRMED) {
                throw new BusinessException(ErrorCode.CONFLICT, "已确认录取，不可更改");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "确认失败，请刷新查看结果后重试");
        }

        if (!now.isBefore(target.getConfirmDeadline())) {
            // 该事务对该异常不回滚，确保 TIMEOUT 状态落库。
            offerMapper.markAbandoned(offerId, now, AbandonReason.TIMEOUT);
            throw new OfferExpiredException();
        }

        if (finalAdmissionMapper.findByStudentId(studentId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "已确认录取，不可更改");
        }

        offerMapper.markConfirmed(offerId, now);
        List<Long> abandonedOfferIds = new ArrayList<>();
        for (Offer other : pending) {
            if (!other.getId().equals(offerId)) {
                offerMapper.markAbandoned(other.getId(), now, AbandonReason.CONFIRMED_OTHER);
                abandonedOfferIds.add(other.getId());
            }
        }

        FinalAdmission finalAdmission = new FinalAdmission();
        finalAdmission.setStudentId(studentId);
        finalAdmission.setTeacherId(target.getTeacherId());
        finalAdmission.setRoundNo(target.getRoundNo());
        finalAdmission.setAdmissionType(AdmissionType.VOLUNTEER);
        finalAdmission.setSourceOfferId(target.getId());
        finalAdmission.setAdmittedAt(now);
        finalAdmissionMapper.insert(finalAdmission);
        log.info("确认拟录取: studentId={}, offerId={}, finalAdmissionId={}, abandonedCount={}",
                studentId, offerId, finalAdmission.getId(), abandonedOfferIds.size());

        LabInfo lab = labInfoService.findByTeacherId(target.getTeacherId());
        return new ConfirmOfferResponse(
                offerId,
                OfferStatus.CONFIRMED,
                now,
                toAdmissionResponse(finalAdmission, lab),
                abandonedOfferIds);
    }

    private AdmissionResponse toAdmissionResponse(FinalAdmission finalAdmission, LabInfo lab) {
        return new AdmissionResponse(
                finalAdmission.getId(),
                finalAdmission.getTeacherId(),
                lab == null ? null : lab.getTeacherName(),
                lab == null ? null : lab.getLabName(),
                lab == null ? null : lab.getLocation(),
                finalAdmission.getRoundNo(),
                finalAdmission.getAdmissionType(),
                finalAdmission.getAdmittedAt());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
