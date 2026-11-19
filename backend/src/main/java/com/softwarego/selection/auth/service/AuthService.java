package com.softwarego.selection.auth.service;

import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.auth.dto.AuthUserResponse;
import com.softwarego.selection.auth.dto.LoginRequest;
import com.softwarego.selection.user.domain.User;
import com.softwarego.selection.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;

    /**
     * 登录：按 (staffNo, role) 查找或创建身份，销毁旧会话后建立新会话。
     */
    public AuthUserResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.findOrCreate(request.staffNo(), request.role());
        SessionUser sessionUser = new SessionUser(
                user.getId(), user.getStaffNo(), user.getName(), user.getRole());

        // 防会话固定：登录成功后旧会话一律作废，再建立新会话。
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionUser.SESSION_KEY, sessionUser);

        log.info("登录成功: userId={}, role={}", sessionUser.userId(), sessionUser.role());
        return toResponse(sessionUser);
    }

    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return;
        }

        Object value = session.getAttribute(SessionUser.SESSION_KEY);
        if (value instanceof SessionUser sessionUser) {
            log.info("注销成功: userId={}, role={}", sessionUser.userId(), sessionUser.role());
        }
        session.invalidate();
    }

    public AuthUserResponse me(SessionUser sessionUser) {
        return toResponse(sessionUser);
    }

    private AuthUserResponse toResponse(SessionUser sessionUser) {
        return new AuthUserResponse(
                sessionUser.userId(), sessionUser.staffNo(), sessionUser.name(), sessionUser.role());
    }
}
