package com.softwarego.selection.user.mapper;

import com.softwarego.selection.user.domain.Role;
import com.softwarego.selection.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByStaffNoAndRole(@Param("staffNo") String staffNo, @Param("role") Role role);

    User findById(@Param("id") Long id);

    int insert(User user);
}
