package com.softwarego.selection.volunteer.mapper;

import com.softwarego.selection.volunteer.domain.ApplicationTarget;
import com.softwarego.selection.volunteer.dto.VolunteerTargetResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApplicationTargetMapper {

    List<VolunteerTargetResponse> findTargetsByApplicationId(
            @Param("applicationId") Long applicationId);

    int deleteByApplicationId(@Param("applicationId") Long applicationId);

    int insert(ApplicationTarget target);

    /**
     * 本轮目标已评分或已产生拟录取（含待确认/已确认/已放弃）时 > 0。
     */
    long countProcessedTargets(@Param("applicationId") Long applicationId);
}
