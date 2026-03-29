package com.azheng.boot.user.enums;

import lombok.Getter;

/**
 * 用户角色类型
 */
@Getter
public enum Role {

    /**
     * 管理员
     */
    ADMIN("admin"),

    /**
     * 普通用户
     */
    USER("user");


    private final String code;

    Role(String code) {
        this.code = code;
    }
}
