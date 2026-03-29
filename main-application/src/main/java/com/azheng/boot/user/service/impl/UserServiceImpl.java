package com.azheng.boot.user.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.user.databaseobject.UserDO;
import com.azheng.boot.user.enums.Role;
import com.azheng.boot.user.mapper.UserMapper;
import com.azheng.boot.user.request.ChangePasswordRequest;
import com.azheng.boot.user.request.CreateUserRequest;
import com.azheng.boot.user.request.UserPageQueryRequest;
import com.azheng.boot.user.request.UserUpdateRequest;
import com.azheng.boot.user.service.UserService;
import com.azheng.boot.user.vo.UserVO;
import com.azheng.framework.context.LoginUser;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class UserServiceImpl implements UserService {
    public static final String DEFAULT_USER_NAME = "admin";

    @Autowired
    private UserMapper userMapper;

    /**
     * 新建用户
     * @param createUserRequest
     * @return
     */
    @Override
    public boolean createUser(CreateUserRequest createUserRequest) {
        // Assert 断言某些对象或值是否符合规定，否则抛出异常
        Assert.notNull(createUserRequest, ()-> new ClientException("请求不能为空"));
        String userName = StrUtil.trimToNull(createUserRequest.getUsername());
        String password = StrUtil.trimToNull(createUserRequest.getPassword());
        Assert.notNull(userName, ()-> new ClientException("用户名不能为空"));
        Assert.notNull(password, ()-> new ClientException("密码不能为空"));
        // 校验用户名唯一性
        ensureUsernameAvailable(userName,null);
        // 判断是否为系统顶级用户名 admin
        if(DEFAULT_USER_NAME.equalsIgnoreCase(userName)){
            throw new ClientException("用户名不合法");
        }
        // 设置角色
        String role = normalizeRole(createUserRequest.getRole());
        //校验邮箱号长度

        UserDO userDO = UserDO.builder()
                .username(userName)
                .password(password)
                .role(role)
                .avatar(StrUtil.trimToNull(createUserRequest.getAvatar()))
                .build();
        try {
            userMapper.insert(userDO);
        }catch (Exception e){
            throw new ClientException("新增用户失败，请稍后重试");
        }
        return true;
    }

    @Override
    public void update(String id, UserUpdateRequest requestParam) {
        Assert.notNull(requestParam, () -> new ClientException("请求不能为空"));
        UserDO record = loadById(id);
        ensureNotDefaultAdmin(record);

        if (requestParam.getUsername() != null) {
            String username = StrUtil.trimToNull(requestParam.getUsername());
            Assert.notBlank(username, () -> new ClientException("用户名不能为空"));
            if (!username.equals(record.getUsername())) {
                if (DEFAULT_USER_NAME.equalsIgnoreCase(username)) {
                    throw new ClientException("默认管理员用户名不可用");
                }
                ensureUsernameAvailable(username, record.getId());
            }
            record.setUsername(username);
        }

        if (requestParam.getRole() != null) {
            record.setRole(normalizeRole(requestParam.getRole()));
        }

        if (requestParam.getAvatar() != null) {
            record.setAvatar(StrUtil.trimToNull(requestParam.getAvatar()));
        }

        if (requestParam.getPassword() != null) {
            String password = StrUtil.trimToNull(requestParam.getPassword());
            Assert.notBlank(password, () -> new ClientException("新密码不能为空"));
            record.setPassword(password);
        }

        userMapper.updateById(record);
    }

    @Override
    public void delete(String id) {
        UserDO record = loadById(id);
        ensureNotDefaultAdmin(record);
        userMapper.deleteById(record.getId());
    }

    @Override
    public void changePassword(ChangePasswordRequest requestParam) {
        Assert.notNull(requestParam, () -> new ClientException("请求不能为空"));
        String current = StrUtil.trimToNull(requestParam.getCurrentPassword());
        String next = StrUtil.trimToNull(requestParam.getNewPassword());
        Assert.notBlank(current, () -> new ClientException("当前密码不能为空"));
        Assert.notBlank(next, () -> new ClientException("新密码不能为空"));

        LoginUser loginUser = UserContext.requireUser();
        UserDO record = userMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getId, parseId(loginUser.getUserId()))
                        .eq(UserDO::getStatus, 1)
        );
        Assert.notNull(record, () -> new ClientException("用户不存在"));
        if (!passwordMatches(current, record.getPassword())) {
            throw new ClientException("当前密码不正确");
        }
        record.setPassword(next);
        userMapper.updateById(record);
    }

    private Long parseId(String id) {
        if (StrUtil.isBlank(id)) {
            throw new ClientException("用户ID不能为空");
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new ClientException("用户ID非法");
        }
    }

    private boolean passwordMatches(String input, String stored) {
        if (stored == null) {
            return input == null;
        }
        return stored.equals(input);
    }

    private UserDO loadById(String id) {
        Long parsedId = parseId(id);
        UserDO record = userMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getId, parsedId)
                        .eq(UserDO::getStatus, 1)
        );
        Assert.notNull(record, () -> new ClientException("用户不存在"));
        return record;
    }

    private void ensureNotDefaultAdmin(UserDO record) {
        if (record != null && DEFAULT_USER_NAME.equalsIgnoreCase(record.getUsername())) {
            throw new ClientException("默认管理员不允许修改或删除");
        }
    }

    private String normalizeRole(String role) {
        String value = StrUtil.trimToNull(role);
        if (StrUtil.isBlank(value)) {
            return Role.USER.getCode();
        }
        if (Role.ADMIN.getCode().equalsIgnoreCase(value)) {
            return Role.ADMIN.getCode();
        }
        if (Role.USER.getCode().equalsIgnoreCase(value)) {
            return Role.USER.getCode();
        }
        throw new ClientException("角色类型不合法");
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        UserDO existing = userMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getUsername, username)
                        .eq(UserDO::getStatus, 1)
                        .ne(excludeId != null, UserDO::getId, excludeId)
        );
        if (existing != null) {
            throw new ClientException("用户名已存在");
        }
    }
    /**
     * 分页查询用户信息
     * @param requestParam
     * @return
     */
    @Override
    public IPage<UserVO> pageQuery(UserPageQueryRequest requestParam) {
        // 去除前端输入的无效部分，并校验null
        String username = StrUtil.trimToNull(requestParam.getUsername());
        //封装Page，设置参数
        Page<UserDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<UserDO> result = userMapper.selectPage(page,
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getStatus, 1)
                        .and(StrUtil.isNotBlank(username), wrapper -> wrapper
                                .like(UserDO::getUsername, username))
                        .orderByDesc(UserDO::getUpdateTime)
        );
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(result, userVO);
        return result.convert(this::toVO);
    }

    /**
     * DO->VO
     * @param record
     * @return
     */
    private UserVO toVO(UserDO record) {
        return UserVO.builder()
                .id(String.valueOf(record.getId()))
                .username(record.getUsername())
                .role(record.getRole())
                .avatar(record.getAvatar())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
