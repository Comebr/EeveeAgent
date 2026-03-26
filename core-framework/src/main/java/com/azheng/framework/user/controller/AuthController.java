package com.azheng.framework.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.azheng.framework.Result;
import com.azheng.framework.user.dto.LoginDto;
import com.azheng.framework.user.service.AuthService;
import com.azheng.framework.user.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
