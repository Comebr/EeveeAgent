package com.azheng.framework.user.dto;

import lombok.Data;

/**
 * 前端请求传递的用户信息
 */
@Data
public class LoginDto {
    //用户登录前端他只能录入用户名和登录密码
    private String username;
    private String password;
}
