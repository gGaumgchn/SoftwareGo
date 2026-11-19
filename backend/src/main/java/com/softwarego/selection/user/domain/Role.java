package com.softwarego.selection.user.domain;

/**
 * 系统身份。身份键为 (staffNo, role)，同一学工号的不同角色是两条独立身份。
 */
public enum Role {
    STUDENT,
    TEACHER,
    MANAGER
}
