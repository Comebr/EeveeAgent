package com.azheng.boot.user.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    /**
     * 当前密码
     */
    private String currentPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
