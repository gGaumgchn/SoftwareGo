package com.softwarego.selection.user.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * sys_user 实体。name 可为空；登录时按 (staff_no, role) 自动创建。
 */
@Getter
@Setter
public class User {

    private Long id;
    private String staffNo;
    private String name;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
