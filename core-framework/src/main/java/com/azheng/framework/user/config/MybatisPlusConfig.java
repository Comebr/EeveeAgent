package com.azheng.framework.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.azheng.framework.user.mapper")
public class MybatisPlusConfig {
}
