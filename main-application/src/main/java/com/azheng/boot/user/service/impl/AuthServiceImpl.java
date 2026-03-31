package com.azheng.boot.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.user.po.UserDO;
import com.azheng.boot.user.mapper.UserMapper;
import com.azheng.boot.user.request.LoginRequest;
import com.azheng.boot.user.service.AuthService;
import com.azheng.boot.user.vo.LoginVO;
import com.azheng.framework.exception.ClientException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    public static final String DEFAULT_AVATAR = "static/default-avatar.png";


    /**
     * 用户登录
     * @param loginRequest
     * @return
     */
    @Override
    public LoginVO login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // 根据用户名获取用户信息
        UserDO user = getUserByName(username);
        // 校验
        if(user == null || !user.getPassword().equals(password)) {
            throw new ClientException( "用户名或密码错误！");
        }

        // 调用登录方法
        String loginId = user.getId().toString();
        if(loginId.isBlank()){
            throw new ClientException("用户信息异常");
        }
        StpUtil.login(loginId);
        //用户头像头像预设
        String avatar = StrUtil.isBlank(user.getAvatar()) ? DEFAULT_AVATAR : user.getAvatar();

        return new LoginVO(loginId, user.getUsername(), user.getRole(), StpUtil.getTokenValue(), avatar);
    }

    /**
     * 退出登录
     */
    @Override
    public void loginOut() {
        StpUtil.logout();
    }


    /**
     * 根据用户名查询用户
     * @param username
     * @return
     */
    public UserDO getUserByName(String username) {
        if(username == null || "".equals(username)){
            return null;
        }
        return userMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getUsername, username)
                        .eq(UserDO::getStatus, 1)
        );
    }

}
