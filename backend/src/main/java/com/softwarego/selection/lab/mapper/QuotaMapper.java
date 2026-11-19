package com.softwarego.selection.lab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface QuotaMapper {

    long countFinalAdmissions(@Param("teacherId") Long teacherId);

    long countPendingOffers(@Param("teacherId") Long teacherId,
                            @Param("now") LocalDateTime now);
}
