package com.azheng.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.azheng.boot.user.mapper")
@ComponentScan(basePackages = {"com.azheng.boot", "com.azheng.agent", "com.azheng.framework"})
public class EeveeApplication {
    public static void main(String[] args) {SpringApplication.run(EeveeApplication.class, args);}
}
