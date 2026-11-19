package com.softwarego.selection.stage.mapper;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import com.softwarego.selection.stage.domain.StageChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StageChangeMapper {

    StageChange findByChangeNo(@Param("changeNo") String changeNo);

    StageChange findByChangeNoForUpdate(@Param("changeNo") String changeNo);

    int insert(StageChange change);

    int markPublished(@Param("id") Long id, @Param("announcementId") Long announcementId);

    int markFailed(@Param("id") Long id);

    List<StageChange> findPage(@Param("status") AnnouncementStatus status,
                               @Param("offset") long offset,
                               @Param("size") int size);

    long count(@Param("status") AnnouncementStatus status);
}
