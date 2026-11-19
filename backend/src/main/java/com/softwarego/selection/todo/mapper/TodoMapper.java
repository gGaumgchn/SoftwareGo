package com.softwarego.selection.todo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    long countStudentApplication(@Param("studentId") Long studentId,
                                 @Param("roundNo") Integer roundNo);

    long countUnscoredTargets(@Param("teacherId") Long teacherId,
                              @Param("roundNo") Integer roundNo);

    long countGrantableTargets(@Param("teacherId") Long teacherId,
                               @Param("roundNo") Integer roundNo);

    long countUnconfiguredStages();
}
