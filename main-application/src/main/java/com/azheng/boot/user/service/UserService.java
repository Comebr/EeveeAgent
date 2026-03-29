package com.azheng.boot.user.service;

import com.azheng.boot.user.request.ChangePasswordRequest;
import com.azheng.boot.user.request.CreateUserRequest;
import com.azheng.boot.user.request.UserPageQueryRequest;
import com.azheng.boot.user.request.UserUpdateRequest;
import com.azheng.boot.user.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface UserService {

    /**
     * 分页查询
     */

    IPage<UserVO> pageQuery(UserPageQueryRequest requestParam);

    /**
     * 创建用户
     */
    boolean createUser(CreateUserRequest createUserRequest);

    /**
     * 更新用户
     */
    void update(String id, UserUpdateRequest requestParam);

    /**
     * 删除用户
     */
    void delete(String id);

    /**
     * 修改当前用户密码
     */
    void changePassword(ChangePasswordRequest requestParam);
}
