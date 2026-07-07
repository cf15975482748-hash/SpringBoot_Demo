package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Mapper.SearchMapper;
import com.SpringBoot_Test.Mapper.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpSession;

@Controller
public class TestController {

    @Autowired
    private SearchMapper searchMapper;

    @Autowired
    private Mappers mappers;

    private static final String TABLE_SESSION_KEY = "CURRENT_TABLE";

    @GetMapping("/")
    public String index(@RequestParam(value = "searchName", required = false) String searchName, Model model, HttpSession session) {
        List<String> tables = searchMapper.getAllTables();
        model.addAttribute("tables", tables);

        if (tables.isEmpty()) {
            model.addAttribute("error", "没有任何表可查询，请新建一个表！");
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("currentTable", "无");
            session.removeAttribute(TABLE_SESSION_KEY);
            return "index";
        }

        // 从 Session 获取当前表
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        
        // 如果 Session 为空或表已不存在，重置为第一个可用表
        if (currentTable == null || !tables.contains(currentTable)) {
            currentTable = tables.get(0);
            session.setAttribute(TABLE_SESSION_KEY, currentTable);
        }

        // 获取用户数据（如果提供了搜索名称，则进行模糊查询，否则获取全部数据）
        List<User> users;
        if (searchName != null && !searchName.trim().isEmpty()) {
            users = mappers.searchByName(currentTable, searchName.trim());
            model.addAttribute("searchName", searchName);
        } else {
            users = searchMapper.findAll(currentTable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentTable", currentTable);
        
        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam("searchName") String searchName, Model model, HttpSession session) {
        return index(searchName, model, session);
    }

    @PostMapping("/switchTable")
    public String switchTable(@RequestParam("tableName") String tableName, HttpSession session) {
        session.setAttribute(TABLE_SESSION_KEY, tableName);
        return "redirect:/";
    }

    @PostMapping("/createTable")
    @Transactional
    public String createTable(@RequestParam("tableName") String tableName, Model model, HttpSession session) {

        if (!Pattern.matches("^[a-zA-Z0-9_]+$", tableName)) {
            model.addAttribute("error", "表名不合法！仅允许使用字母、数字和下划线。");
            model.addAttribute("showCreateModal", true);
            return index(null, model, session);
        }

        if (searchMapper.checkTableExists(tableName) > 0) {
            model.addAttribute("error", "表名 [" + tableName + "] 已存在，请重新输入！");
            model.addAttribute("showCreateModal", true);
            return index(null, model, session);
        }

        searchMapper.createTable(tableName);
        
        // 插入初始值 1-60 (仅 ID)
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            ids.add((long) i);
        }
        searchMapper.insertInitialIds(tableName, ids);
        session.setAttribute(TABLE_SESSION_KEY, tableName);
        return "redirect:/";
    }

    @PostMapping("/deleteTable")
    public String deleteTable(@RequestParam("tableName") String tableName, HttpSession session) {
        searchMapper.dropTable(tableName);
        String currentTableInSession = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (tableName.equals(currentTableInSession)) {
            session.removeAttribute(TABLE_SESSION_KEY);
        }
        return "redirect:/";
    }

    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user, Model model, HttpSession session) {
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        try {
            String error = validateUser(user);
            if (error != null) {
                model.addAttribute("error", error);
                model.addAttribute("showAddDataModal", true);
                return index(null, model, session);
            }
            mappers.update(currentTable, user);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "添加失败：" + e.getMessage());
            return index(null, model, session);
        }
    }

    @PostMapping("/updateUser")
    public String updateUser(@ModelAttribute User user, Model model, HttpSession session) {
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        try {
            String error = validateUser(user);
            if (error != null) {
                model.addAttribute("error", error);
                model.addAttribute("showEditDataModal", true);
                model.addAttribute("failedUser", user);
                return index(null, model, session);
            }
            mappers.update(currentTable, user);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "更新失败：" + e.getMessage());
            return index(null, model, session);
        }
    }

    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam("id") Long id, HttpSession session) {
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        User emptyUser = new User();
        emptyUser.setId(id);
        emptyUser.setName(null);
        emptyUser.setAge(null);
        mappers.update(currentTable, emptyUser);
        return "redirect:/";
    }

    private String validateUser(User user) {
        // ID 校验 (1-60)
        if (user.getId() == null || user.getId() < 1 || user.getId() > 60) {
            return "ID 必须在 1-60 之间！";
        }
        // 年龄校验 (如果填写了则校验范围 1-30)
        if (user.getAge() != null && (user.getAge() < 1 || user.getAge() > 30)) {
            return "年龄必须在 1-30 之间！";
        }
        // 姓名非法字符检测 (！！@#￥%……&*——+)
        if (user.getName() != null && !user.getName().isEmpty()) {
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

