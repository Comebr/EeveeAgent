package com.azheng.boot.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.azheng.boot.user.request.ChangePasswordRequest;
import com.azheng.boot.user.request.CreateUserRequest;
import com.azheng.boot.user.request.UserPageQueryRequest;
import com.azheng.boot.user.request.UserUpdateRequest;
import com.azheng.boot.user.service.UserService;
import com.azheng.boot.user.vo.CurrentUserVO;
import com.azheng.boot.user.vo.UserVO;
import com.azheng.framework.context.LoginUser;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台-用户管理CRUD
 */
@RestController
@RequestMapping("/api/management")
public class UserController {
    @Autowired
    private  UserService userService;

    @PostMapping("/pageQuery")
    public Result<IPage<UserVO>> userPageQuery(UserPageQueryRequest userPageQueryRequest) {
        StpUtil.checkRole("admin");
       return Results.success(userService.pageQuery(userPageQueryRequest));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<CurrentUserVO> currentUser() {
        LoginUser user = UserContext.requireUser();
        return Results.success(new CurrentUserVO(
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.getAvatar()
        ));
    }

    /**
     * 创建用户
     * @param createUserRequest
     * @return
     */
    @PostMapping("/create")
    public Result<Boolean> createUser(@RequestBody CreateUserRequest createUserRequest) {
        StpUtil.checkRole("admin");
        return Results.success(userService.createUser(createUserRequest));
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody UserUpdateRequest requestParam) {
        StpUtil.checkRole("admin");
        userService.update(id, requestParam);
        return Results.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable String id) {
        StpUtil.checkRole("admin");
        userService.delete(id);
        return Results.success();
    }

    /**
     * 修改当前用户密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest requestParam) {
        userService.changePassword(requestParam);
        return Results.success();
    }
}
