package com.azheng.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(basePackages ={
        "com.azheng.boot.user.mapper",
        "com.azheng.boot.kb.dao.mapper"})
@ComponentScan(basePackages = {"com.azheng.boot", "com.azheng.agent", "com.azheng.framework"})
public class EeveeApplication {
    public static void main(String[] args) {SpringApplication.run(EeveeApplication.class, args);}
}
