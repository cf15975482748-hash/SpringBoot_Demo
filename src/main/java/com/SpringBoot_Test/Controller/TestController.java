package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
public class TestController {


    // ====================== 1. 读取配置文件 ======================
    @Value("${name}")
    private String configName;

    @GetMapping("/config")
    public String getConfig() {
        return "配置文件内容：" + configName;
    }

    // ====================== 2. 读取数据库 ======================
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/users")
    public List<User> getUsers() {
        return userMapper.findAll();
    }
}

