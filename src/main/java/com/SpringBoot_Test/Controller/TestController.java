package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;


@Controller
public class TestController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/")
    public String index(Model model) {
        // 获取所有用户数据
        List<User> users = userMapper.findAll();
        // 将数据传递给前端模板
        model.addAttribute("users", users);
        // 返回 templates 目录下的 index.html
        return "index";
    }
}

