package com.azheng.boot.user.request;

import lombok.Data;

/**
 * 登录时前端传来的参数
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
