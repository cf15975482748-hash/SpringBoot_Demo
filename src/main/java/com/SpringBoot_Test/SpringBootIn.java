package com.SpringBoot_Test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 扫描 Mapper 接口
@MapperScan("com.SpringBoot_Test.Mapper")
public class SpringBootIn {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootIn.class, args);
    }
}