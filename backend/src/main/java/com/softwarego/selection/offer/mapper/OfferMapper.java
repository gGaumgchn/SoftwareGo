package com.softwarego.selection.offer.mapper;

import com.softwarego.selection.offer.domain.AbandonReason;
import com.softwarego.selection.offer.domain.Offer;
import com.softwarego.selection.offer.dto.OfferItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OfferMapper {

    Offer findById(@Param("id") Long id);

    List<Offer> findPendingByStudentForUpdate(@Param("studentId") Long studentId);

    List<OfferItemResponse> findByStudentId(@Param("studentId") Long studentId);

    int markConfirmed(@Param("id") Long id, @Param("confirmedAt") LocalDateTime confirmedAt);

    int markAbandoned(@Param("id") Long id,
                      @Param("abandonedAt") LocalDateTime abandonedAt,
                      @Param("reason") AbandonReason reason);

    int expirePendingForStudent(@Param("studentId") Long studentId,
                                @Param("now") LocalDateTime now);

    int expireAllPending(@Param("now") LocalDateTime now);

    int expirePendingForTeacherAndRound(@Param("teacherId") Long teacherId,
                                        @Param("roundNo") Integer roundNo,
                                        @Param("now") LocalDateTime now);

    long countBlockingOffers(@Param("studentId") Long studentId);
}
