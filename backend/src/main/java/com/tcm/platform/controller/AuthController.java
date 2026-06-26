package com.tcm.platform.controller;

import com.tcm.platform.common.Result;
import com.tcm.platform.dto.LoginRequest;
import com.tcm.platform.dto.LoginResponse;
import com.tcm.platform.dto.DepartmentResponse;
import com.tcm.platform.dto.DoctorApplicationRequest;
import com.tcm.platform.dto.DoctorApplicationResponse;
import com.tcm.platform.dto.RegisterRequest;
import com.tcm.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", authService.registerPatient(request));
    }

    @GetMapping("/departments")
    public Result<List<DepartmentResponse>> departments() {
        return Result.success(authService.listEnabledDepartments());
    }

    @PostMapping("/doctor-applications")
    public Result<DoctorApplicationResponse> applyDoctor(
            @Valid @RequestBody DoctorApplicationRequest request
    ) {
        return Result.success("申请已提交，请等待管理员审核", authService.applyDoctor(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/login/patient")
    public Result<LoginResponse> loginPatient(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.loginPatient(request));
    }

    @PostMapping("/login/admin")
    public Result<LoginResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.loginAdmin(request));
    }
}
