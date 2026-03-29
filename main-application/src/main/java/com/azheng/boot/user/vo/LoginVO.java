package com.azheng.boot.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录时前端需要的用户信息：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {

    private String userId;

    private String username;

    private String role;

    private String token;

    private String avatar;

}
