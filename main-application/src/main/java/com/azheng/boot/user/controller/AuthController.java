package com.azheng.boot.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.azheng.framework.Result;
import com.azheng.boot.user.dto.LoginDto;
import com.azheng.boot.user.service.AuthService;
import com.azheng.boot.user.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody LoginDto loginDTO) {
        try {
            LoginVo login = authService.login(loginDTO);
            return Result.success(login);
        }catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
