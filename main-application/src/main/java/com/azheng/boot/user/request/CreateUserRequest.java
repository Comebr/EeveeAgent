package com.azheng.boot.user.request;

import lombok.Data;

/**
 * 前端新建用户
 */
@Data
public class CreateUserRequest {

    private String username;
    private String password;
    private String role;
    private String avatar;

}
