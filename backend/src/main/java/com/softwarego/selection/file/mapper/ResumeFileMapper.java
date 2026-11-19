package com.softwarego.selection.file.mapper;

import com.softwarego.selection.file.domain.ResumeFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResumeFileMapper {

    int insert(ResumeFile resumeFile);

    ResumeFile findById(@Param("id") Long id);

    /**
     * 判断导师是否收到过该学生的申请（用于学生简历下载权限）。
     */
    long countTeacherApplicationTargets(@Param("teacherId") Long teacherId,
                                        @Param("studentId") Long studentId);
}
