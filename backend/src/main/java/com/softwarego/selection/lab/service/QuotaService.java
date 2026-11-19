package com.softwarego.selection.lab.service;

import com.softwarego.selection.lab.mapper.QuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 名额占用统一口径：最终录取（含调剂）+ 未过 confirm_deadline 的 PENDING offer。
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaMapper quotaMapper;
    private final Clock clock;

    public long occupied(Long teacherId) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return quotaMapper.countFinalAdmissions(teacherId)
                + quotaMapper.countPendingOffers(teacherId, now);
    }

    public Long remaining(Integer plannedQuota, long occupied) {
        if (plannedQuota == null) {
            return null;
        }
        return (long) plannedQuota - occupied;
    }
}
