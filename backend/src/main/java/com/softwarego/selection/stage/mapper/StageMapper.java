package com.softwarego.selection.stage.mapper;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StageMapper {

    List<Stage> findAll();

    List<Stage> findAllForUpdate();

    int updateTimes(@Param("stageCode") StageCode stageCode,
                    @Param("startAt") LocalDateTime startAt,
                    @Param("endAt") LocalDateTime endAt);
}
