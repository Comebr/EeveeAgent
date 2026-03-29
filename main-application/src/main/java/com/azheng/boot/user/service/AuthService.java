package com.azheng.boot.user.service;

import com.azheng.boot.user.request.LoginRequest;
import com.azheng.boot.user.vo.LoginVO;

/**
 * 定义用户登录相关的各种方法
 */
public interface AuthService{
    //    ——————————————————登录界面方法——————————————————
    //用户登录
    LoginVO login(LoginRequest loginRequest);

    //退出登录
    void loginOut();


    //    ——————————————————后台用户增删改查方法——————————————————
}
