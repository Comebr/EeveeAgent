package com.azheng.boot.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户管理页面展示的信息 ——后端传给前端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    //前端用字符类型
    private String id;
    //用户名
    private String username;
    //角色
    private String role;
    //头像
    private String avatar;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
}
