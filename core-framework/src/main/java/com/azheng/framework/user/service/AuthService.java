package com.azheng.framework.user.service;

import com.azheng.framework.user.Do.UserDo;
import com.azheng.framework.user.dto.LoginDto;
import com.azheng.framework.user.vo.LoginVo;
import com.baomidou.mybatisplus.extension.service.IService;


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
