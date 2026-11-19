package com.softwarego.selection.announcement.mapper;

import com.softwarego.selection.announcement.domain.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    int insert(Announcement announcement);

    List<Announcement> findPage(@Param("offset") long offset, @Param("size") int size);

    long count();

    Announcement findById(@Param("id") Long id);
}
