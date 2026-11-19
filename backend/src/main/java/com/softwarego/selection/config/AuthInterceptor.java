package com.softwarego.selection.config;

import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.user.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 会话与路径级角色校验。数据归属仍必须在 Service 层按当前用户再校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String HEALTH_PATH = "/health";
    private static final String STUDENT_PREFIX = "/api/v1/student";
    private static final String TEACHER_PREFIX = "/api/v1/teacher";
    private static final String MANAGER_PREFIX = "/api/v1/manager";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 静态资源 / error dispatch 等非 Controller 请求不在这里处理。
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String path = resolvePath(request);
        if (isPublicRequest(request.getMethod(), path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionUser.SESSION_KEY);
        if (!(value instanceof SessionUser sessionUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Role requiredRole = requiredRole(path);
        if (requiredRole != null && requiredRole != sessionUser.role()) {
            String message = requiredRole == Role.MANAGER
                    ? "仅负责人可配置阶段"
                    : "无权访问该数据";
            throw new BusinessException(ErrorCode.FORBIDDEN, message);
        }
        return true;
    }

    private String resolvePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    private boolean isPublicRequest(String method, String path) {
        return ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path))
                || ("GET".equalsIgnoreCase(method) && HEALTH_PATH.equals(path));
    }

    private Role requiredRole(String path) {
        if (path.equals(STUDENT_PREFIX) || path.startsWith(STUDENT_PREFIX + "/")) {
            return Role.STUDENT;
        }
        if (path.equals(TEACHER_PREFIX) || path.startsWith(TEACHER_PREFIX + "/")) {
            return Role.TEACHER;
        }
        if (path.equals(MANAGER_PREFIX) || path.startsWith(MANAGER_PREFIX + "/")) {
            return Role.MANAGER;
        }
        return null;
    }
}
