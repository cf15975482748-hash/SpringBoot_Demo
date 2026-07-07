package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Mapper.SearchMapper;
import com.SpringBoot_Test.Mapper.UpdatedMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
public class TestController {

    @Autowired
    private SearchMapper searchMapper;

    @Autowired
    private UpdatedMapper updatedMapper;

    private String currentTable = "user";

    @GetMapping("/")
    public String index(Model model) {
        List<String> tables = searchMapper.getAllTables();
        model.addAttribute("tables", tables);

        if (tables.isEmpty()) {
            model.addAttribute("error", "没有任何表可查询，请新建一个表！");
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("currentTable", "无");
            return "index";
        }

        // 如果当前表不在列表里，默认选第一个
        if (!tables.contains(currentTable)) {
            currentTable = tables.get(0);
        }

        // 获取所有用户数据
        List<User> users = searchMapper.findAll(currentTable);
        // 将数据传递给前端模板
        model.addAttribute("users", users);
        model.addAttribute("currentTable", currentTable);
        // 返回 templates 目录下的 index.html
        return "index";
    }

    @PostMapping("/switchTable")
    public String switchTable(@RequestParam("tableName") String tableName) {
        this.currentTable = tableName;
        return "redirect:/";
    }

    @PostMapping("/createTable")
    public String createTable(@RequestParam("tableName") String tableName, Model model) {
        if (searchMapper.checkTableExists(tableName) > 0) {
            model.addAttribute("error", "表名 [" + tableName + "] 已存在，请重新输入！");
            model.addAttribute("showCreateModal", true); // 用于前端自动弹出模态框
            return index(model);
        }

        searchMapper.createTable(tableName);
        
        // 插入初始值 1-60 (仅 ID)
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            ids.add((long) i);
        }
        searchMapper.insertInitialIds(tableName, ids);
        this.currentTable = tableName;
        return "redirect:/";
    }

    @PostMapping("/deleteTable")
    public String deleteTable(@RequestParam("tableName") String tableName) {
        searchMapper.dropTable(tableName);
        if (currentTable.equals(tableName)) {
            currentTable = "user";
        }
        return "redirect:/";
    }

    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user, Model model) {
        String error = validateUser(user);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("showAddDataModal", true); // 验证失败弹出框不关闭
            return index(model);
        }
        // 因为 ID 1-60 已经存在，所以“添加数据”实际上是更新对应 ID 的行
        updatedMapper.update(currentTable, user);
        return "redirect:/";
    }

    @PostMapping("/updateUser")
    public String updateUser(@ModelAttribute User user, Model model) {
        String error = validateUser(user);
        if (error != null) {
            model.addAttribute("error", error);
            return index(model);
        }
        updatedMapper.update(currentTable, user);
        return "redirect:/";
    }

    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam("id") Long id) {
        // 删除数据将 ID 对应的 name 和 age 置空，而不是物理删除行（保持 1-60 ID）
        User emptyUser = new User();
        emptyUser.setId(id);
        emptyUser.setName(null);
        emptyUser.setAge(null);
        updatedMapper.update(currentTable, emptyUser);
        return "redirect:/";
    }

    private String validateUser(User user) {
        // ID 校验 (1-60)
        if (user.getId() == null || user.getId() < 1 || user.getId() > 60) {
            return "ID 必须在 1-60 之间！";
        }
        // 年龄校验 (1-30)
        if (user.getAge() == null || user.getAge() < 1 || user.getAge() > 30) {
            return "年龄必须在 1-30 之间！";
        }
        // 姓名非法字符检测 (！！@#￥%……&*——+)
        if (user.getName() != null) {
            String illegalChars = "！！@#￥%……&*——+";
            for (char c : illegalChars.toCharArray()) {
                if (user.getName().indexOf(c) != -1) {
                    return "姓名包含非法字符（" + illegalChars + "）！";
                }
            }
        }
        return null;
    }
}

