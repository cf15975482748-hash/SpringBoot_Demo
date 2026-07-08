package com.SpringBoot_Test;

import com.SpringBoot_Test.Mapper.SearchMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
// 扫描 Mapper 接口
@MapperScan("com.SpringBoot_Test.Mapper")
public class SpringBootIn {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootIn.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(SearchMapper searchMapper) {
        return args -> {
            // 初始化权限表
            searchMapper.createAdminTable();
            searchMapper.createTeacherTable();
            searchMapper.createStudentTable();
            // 初始化默认管理员
            searchMapper.initDefaultAdmin();
            System.out.println("数据库权限表初始化完成。");
        };
    }
}