package com.softwarego.selection.auth;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 从 Session 解析 {@link CurrentUser} 参数。AuthInterceptor 已做第一道校验，
 * 这里再兜底一次，避免 Controller 拿到空用户。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && SessionUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpSession session = request == null ? null : request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionUser.SESSION_KEY);
        if (value instanceof SessionUser sessionUser) {
            return sessionUser;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
