package com.azheng.boot.user.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 用户分页查询
 */
@Data
public class UserPageQueryRequest extends Page {

    //根据用户名模糊匹配
    private String username;
}
