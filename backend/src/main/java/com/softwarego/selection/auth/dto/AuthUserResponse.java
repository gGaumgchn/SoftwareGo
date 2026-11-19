package com.softwarego.selection.auth.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.user.domain.Role;

/**
 * 登录响应 / 当前用户响应。name 可为空，前端为空时显示 staffNo。
 */
@JsonPropertyOrder({"userId", "staffNo", "name", "role"})
public record AuthUserResponse(Long userId, String staffNo, String name, Role role) {
}
