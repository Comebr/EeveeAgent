package com.azheng.boot.user.service;

import com.azheng.boot.user.dto.LoginDto;
import com.azheng.boot.user.vo.LoginVo;

/**
 * 定义用户登录相关的各种方法
 */
public interface AuthService{
    //    ——————————————————登录界面方法——————————————————
    //用户登录
    LoginVo login(LoginDto loginDTO);

    //退出登录
    void loginOut();


    //    ——————————————————后台用户增删改查方法——————————————————
}
