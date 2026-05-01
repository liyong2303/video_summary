package com.videosummary.controller;

import com.videosummary.dto.ApiResult;
import com.videosummary.dto.LoginRequest;
import com.videosummary.dto.RegisterRequest;
import com.videosummary.dto.UserInfoResponse;
import com.videosummary.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResult<Map<String, String>> register(@RequestBody @Valid RegisterRequest request) {
        String token = authService.register(request.getUsername(), request.getPassword());
        return ApiResult.success(Map.of("token", token));
    }

    @PostMapping("/login")
    public ApiResult<Map<String, String>> login(@RequestBody @Valid LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ApiResult.success(Map.of("token", token));
    }

    @GetMapping("/me")
    public ApiResult<UserInfoResponse> me() {
        return ApiResult.success(authService.getCurrentUser());
    }
}
