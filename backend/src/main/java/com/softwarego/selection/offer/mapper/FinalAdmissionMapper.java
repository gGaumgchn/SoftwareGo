package com.softwarego.selection.offer.mapper;

import com.softwarego.selection.offer.domain.FinalAdmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinalAdmissionMapper {

    FinalAdmission findByStudentId(@Param("studentId") Long studentId);

    int insert(FinalAdmission finalAdmission);
}
