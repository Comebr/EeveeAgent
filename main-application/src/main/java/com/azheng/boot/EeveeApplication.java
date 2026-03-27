package com.azheng.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.azheng.boot.user.mapper")
public class EeveeApplication {
    public static void main(String[] args) {SpringApplication.run(EeveeApplication.class, args);}
}
