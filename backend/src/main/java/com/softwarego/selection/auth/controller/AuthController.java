package com.softwarego.selection.auth.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.auth.dto.AuthUserResponse;
import com.softwarego.selection.auth.dto.LoginRequest;
import com.softwarego.selection.auth.service.AuthService;
import com.softwarego.selection.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthUserResponse> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<AuthUserResponse> me(@CurrentUser SessionUser sessionUser) {
        return Result.ok(authService.me(sessionUser));
    }
}
