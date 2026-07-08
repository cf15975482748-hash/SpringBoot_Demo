package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Mapper.TableAuthMapper;
import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Mapper.SearchMapper;
import com.SpringBoot_Test.Mapper.Mappers;
import com.SpringBoot_Test.Util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpSession;

@Controller
public class TestController {

    @Autowired
    private SearchMapper searchMapper;

    @Autowired
    private Mappers mappers;

    @Autowired
    private TableAuthMapper tableAuthMapper;

    private static final String TABLE_SESSION_KEY = "CURRENT_TABLE";

    @GetMapping("/")
    public String index(@RequestParam(value = "searchName", required = false) String searchName, Model model, HttpSession session) {
        List<String> visibleTables = new ArrayList<>();
        String role = (String) session.getAttribute(AuthUtil.SESSION_ROLE);
        
        for (String tableName : searchMapper.getAllTables()) {
            if ("admin".equals(role) || isBusinessTable(tableName)) visibleTables.add(tableName);
        }
        
        model.addAttribute("tables", visibleTables);
        if (visibleTables.isEmpty()) {
            model.addAttribute("error", "没有任何表可查询！");
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("currentTable", "无");
            return "index";
        }

        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null || !visibleTables.contains(currentTable)) {
            currentTable = visibleTables.get(0);
            session.setAttribute(TABLE_SESSION_KEY, currentTable);
        }

        List<User> users = (searchName != null && !searchName.trim().isEmpty()) ?
                mappers.searchByName(currentTable, searchName.trim()) :
                searchMapper.findAll(currentTable);
        
        if (isBusinessTable(currentTable)) {
            try { model.addAttribute("tableAuthInfo", tableAuthMapper.getTableAuthInfo(currentTable)); } catch (Exception ignored) {}
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentTable", currentTable);
        if (searchName != null) model.addAttribute("searchName", searchName);
        return "index";
    }

    private boolean isBusinessTable(String tableName) {
        return !tableName.equals("admin") && !tableName.equals("teacher") && !tableName.equals("student");
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
        if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "学生无权建表！");
        } else if (!Pattern.matches("^[a-zA-Z0-9_]+$", tableName)) {
            model.addAttribute("error", "表名不合法！");
            model.addAttribute("showCreateModal", true);
        } else {
            try {
                if (searchMapper.checkTableExists(tableName) > 0) {
                    ensureTableStructure(tableName);
                    model.addAttribute("error", "表已存在，结构已同步。");
                } else {
                    searchMapper.createTable(tableName);
                    List<Long> ids = new ArrayList<>();
                    for (int i = 1; i <= 60; i++) ids.add((long) i);
                    searchMapper.insertInitialIds(tableName, ids, (String) session.getAttribute("username"));
                }
                session.setAttribute(TABLE_SESSION_KEY, tableName);
                return "redirect:/";
            } catch (Exception e) {
                model.addAttribute("error", "操作失败：" + e.getMessage());
                model.addAttribute("showCreateModal", true);
            }
        }
        return index(null, model, session);
    }

    // 辅助方法：确保表结构包含权限字段
    private void ensureTableStructure(String tableName) {
        try {
            if (searchMapper.checkColumnExists(tableName, "create_user") == 0) {
                searchMapper.addColumnToTable(tableName, "create_user", "VARCHAR(100)");
            }
            if (searchMapper.checkColumnExists(tableName, "allow_student") == 0) {
                searchMapper.addColumnToTable(tableName, "allow_student", "TEXT");
            }
        } catch (Exception ignored) {}
    }

    @PostMapping("/deleteTable")
    public String deleteTable(@RequestParam("tableName") String tableName, Model model, HttpSession session) {
        if (!isBusinessTable(tableName)) {
            model.addAttribute("error", "系统表禁止删除！");
        } else if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "学生无权删除表！");
        } else {
            searchMapper.dropTable(tableName);
            if (tableName.equals(session.getAttribute(TABLE_SESSION_KEY))) session.removeAttribute(TABLE_SESSION_KEY);
            return "redirect:/";
        }
        return index(null, model, session);
    }

    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user, Model model, HttpSession session) {
        return handleDataUpdate(user, "showAddDataModal", model, session);
    }

    @PostMapping("/updateUser")
    public String updateUser(@ModelAttribute User user, Model model, HttpSession session) {
        return handleDataUpdate(user, "showEditDataModal", model, session);
    }

    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam("id") Long id, Model model, HttpSession session) {
        User user = new User(); user.setId(id);
        return handleDataUpdate(user, null, model, session);
    }

    private String handleDataUpdate(User user, String modalFlag, Model model, HttpSession session) {
        if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "权限不足！");
            return index(null, model, session);
        }
        String table = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (table == null || !hasWriteAuth(table, session)) {
            model.addAttribute("error", "无权操作该表！");
            return index(null, model, session);
        }
        try {
            String error = validateUser(user);
            if (error != null) {
                model.addAttribute("error", error);
                if (modalFlag != null) model.addAttribute(modalFlag, true);
                if ("showEditDataModal".equals(modalFlag)) model.addAttribute("failedUser", user);
                return index(null, model, session);
            }
            mappers.update(table, user);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "操作失败：" + e.getMessage());
            return index(null, model, session);
        }
    }

    private boolean hasWriteAuth(String tableName, HttpSession session) {
        if (AuthUtil.isAdmin(session)) return true;
        if (AuthUtil.isTeacher(session)) {
            // 老师有权限更改所有业务表（非 admin/teacher/student）
            return isBusinessTable(tableName);
        }
        return false;
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

