package com.azheng.boot.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.azheng.boot.user.mapper")
public class MybatisPlusConfig {
}
