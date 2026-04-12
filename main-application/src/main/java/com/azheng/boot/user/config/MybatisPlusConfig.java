package com.azheng.boot.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"com.azheng.boot.user.mapper", "com.azheng.boot.intent.dao.mapper"})
public class MybatisPlusConfig {
}
