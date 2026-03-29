package com.azheng.boot.user.databaseobject;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author azheng
 * 用户实体类：对应数据库表字段
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "user")
public class UserDO {
    //

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    private String avatar;//用户头像
    private String role;  //角色

    private String email;
    private String phone;
    private Integer status;
    //插入时填充
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    //更新是填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;



}
