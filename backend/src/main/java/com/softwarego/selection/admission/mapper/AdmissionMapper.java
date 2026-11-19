package com.softwarego.selection.admission.mapper;

import com.softwarego.selection.admission.domain.AdmissionTarget;
import com.softwarego.selection.admission.domain.GrantCandidate;
import com.softwarego.selection.admission.dto.TeacherApplicationItem;
import com.softwarego.selection.offer.domain.Offer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdmissionMapper {

    List<TeacherApplicationItem> findApplications(@Param("teacherId") Long teacherId,
                                                  @Param("roundNo") Integer roundNo);

    AdmissionTarget findTarget(@Param("targetId") Long targetId);

    int updateScore(@Param("targetId") Long targetId,
                    @Param("score") BigDecimal score,
                    @Param("scoredAt") LocalDateTime scoredAt);

    int updateTieOrder(@Param("targetId") Long targetId,
                       @Param("tieOrder") Integer tieOrder,
                       @Param("updatedAt") LocalDateTime updatedAt);

    long countTieOrderConflict(@Param("teacherId") Long teacherId,
                               @Param("roundNo") Integer roundNo,
                               @Param("score") BigDecimal score,
                               @Param("tieOrder") Integer tieOrder,
                               @Param("targetId") Long targetId);

    long countFinalAdmissions(@Param("teacherId") Long teacherId);

    long countPendingOffers(@Param("teacherId") Long teacherId,
                            @Param("now") LocalDateTime now);

    long countOfferByTargetId(@Param("targetId") Long targetId);

    List<GrantCandidate> findGrantCandidates(@Param("teacherId") Long teacherId,
                                            @Param("roundNo") Integer roundNo,
                                            @Param("cutoffScore") BigDecimal cutoffScore);

    int insertOffer(Offer offer);
}
