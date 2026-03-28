package com.azheng.boot.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.user.Do.UserDo;
import com.azheng.boot.user.dto.LoginDto;
import com.azheng.boot.user.mapper.userMapper;
import com.azheng.boot.user.service.AuthService;
import com.azheng.boot.user.vo.LoginVo;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.errorcode.BaseErrorCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final userMapper userMapper;
    public static final String DEFAULT_AVATAR = "static/default-avatar.png";


    /**
     * 用户登录
     * @param loginDTO
     * @return
     */
    @Override
    public LoginVo login(LoginDto loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        // 根据用户名获取用户信息
        UserDo user = getUserByName(username);
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

        return new LoginVo(loginId,user.getRole(),StpUtil.getTokenValue(),avatar);
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
    public UserDo getUserByName(String username) {
        if(username == null || "".equals(username)){
            return null;
        }
        return userMapper.selectOne(
                Wrappers.lambdaQuery(UserDo.class)
                        .eq(UserDo::getUsername, username)
                        .eq(UserDo::getStatus, 1)
        );
    }

}
