package com.softwarego.selection.volunteer.mapper;

import com.softwarego.selection.volunteer.domain.VolunteerApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VolunteerApplicationMapper {

    VolunteerApplication findByStudentAndRound(@Param("studentId") Long studentId,
                                               @Param("roundNo") Integer roundNo);

    VolunteerApplication findByStudentAndRoundForUpdate(@Param("studentId") Long studentId,
                                                        @Param("roundNo") Integer roundNo);

    List<VolunteerApplication> findByStudent(@Param("studentId") Long studentId,
                                             @Param("roundNo") Integer roundNo);

    int insert(VolunteerApplication application);

    int update(VolunteerApplication application);

    long countFinalAdmissions(@Param("studentId") Long studentId);
}
