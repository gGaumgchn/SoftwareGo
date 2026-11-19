package com.softwarego.selection.auth;

import com.softwarego.selection.user.domain.Role;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话中的当前用户。只保存前端展示与路径角色校验所需字段，不保存密码、Session ID。
 */
public record SessionUser(Long userId, String staffNo, String name, Role role) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "softwarego.session.user";
}
