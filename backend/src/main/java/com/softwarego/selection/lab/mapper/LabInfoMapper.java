package com.softwarego.selection.lab.mapper;

import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.dto.AvailableTeacherItem;
import com.softwarego.selection.lab.dto.LabListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LabInfoMapper {

    LabInfo findById(@Param("id") Long id);

    LabInfo findByTeacherId(@Param("teacherId") Long teacherId);

    LabInfo findByTeacherIdForUpdate(@Param("teacherId") Long teacherId);

    int insert(LabInfo labInfo);

    int update(LabInfo labInfo);

    List<LabListItem> findPage(@Param("keyword") String keyword,
                               @Param("researchDirection") String researchDirection,
                               @Param("offset") long offset,
                               @Param("size") int size);

    long count(@Param("keyword") String keyword,
               @Param("researchDirection") String researchDirection);

    List<String> findResearchDirections();

    long sumQuotaByLabNameAndLocation(@Param("labName") String labName,
                                      @Param("location") String location);

    List<LabInfo> findByTeacherName(@Param("teacherName") String teacherName);

    List<AvailableTeacherItem> findAvailableTeachers(@Param("now") LocalDateTime now);
}
