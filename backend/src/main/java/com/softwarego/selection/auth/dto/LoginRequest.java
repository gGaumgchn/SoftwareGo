package com.softwarego.selection.auth.dto;

import com.softwarego.selection.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 登录请求。只校验角色必选与学工号格式，不查询账号是否存在。
 */
public record LoginRequest(

        @NotNull(message = "请选择身份")
        Role role,

        @NotBlank(message = "学工号异常")
        @Pattern(regexp = "^\\d{10}$", message = "学工号异常")
        String staffNo
) {
}
