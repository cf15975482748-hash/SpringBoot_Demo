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
        List<String> allTables = searchMapper.getAllTables();
        List<String> visibleTables = new ArrayList<>();
        
        String role = (String) session.getAttribute(AuthUtil.SESSION_ROLE);
        String username = (String) session.getAttribute("username");

        for (String tableName : allTables) {
            if ("admin".equals(role)) {
                visibleTables.add(tableName);
            } else if ("teacher".equals(role) || "student".equals(role)) {
                // 老师和学生都能看到所有业务表（非 admin/teacher/student）
                if (isBusinessTable(tableName)) {
                    visibleTables.add(tableName);
                }
            }
        }
        
        model.addAttribute("tables", visibleTables);

        if (visibleTables.isEmpty()) {
            model.addAttribute("error", "没有任何表可查询！");
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("currentTable", "无");
            session.removeAttribute(TABLE_SESSION_KEY);
            return "index";
        }

        // 从 Session 获取当前表
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        
        // 如果 Session 为空或表已不再可见，重置为第一个可用表
        if (currentTable == null || !visibleTables.contains(currentTable)) {
            currentTable = visibleTables.get(0);
            session.setAttribute(TABLE_SESSION_KEY, currentTable);
        }

        // 获取用户数据
        List<User> users;
        if (searchName != null && !searchName.trim().isEmpty()) {
            users = mappers.searchByName(currentTable, searchName.trim());
            model.addAttribute("searchName", searchName);
        } else {
            users = searchMapper.findAll(currentTable);
        }
        
        // 如果是权限表，获取白名单信息供前端显示（仅管理员或该表创建者老师可见）
        if (isBusinessTable(currentTable)) {
            try {
                Map<String, Object> authInfo = tableAuthMapper.getTableAuthInfo(currentTable);
                model.addAttribute("tableAuthInfo", authInfo);
            } catch (Exception ignored) {}
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentTable", currentTable);
        
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
            model.addAttribute("error", "权限不足：学生账号无法创建新表。");
            return index(null, model, session);
        }

        // 严格校验表名
        if (!Pattern.matches("^[a-zA-Z0-9_]+$", tableName)) {
            model.addAttribute("error", "新建表失败：表名不合法。仅允许使用字母、数字和下划线。");
            model.addAttribute("showCreateModal", true);
            return index(null, model, session);
        }

        try {
            // 1. 检查是否已存在
            if (searchMapper.checkTableExists(tableName) > 0) {
                // 如果表已存在，检查是否缺失权限字段，如果缺失则尝试补全
                ensureTableStructure(tableName);
                model.addAttribute("error", "提示：表名 [" + tableName + "] 已存在，系统已自动同步结构。");
                session.setAttribute(TABLE_SESSION_KEY, tableName);
                return "redirect:/";
            }

            // 2. 独立执行建表语句 (DDL)
            try {
                searchMapper.createTable(tableName);
            } catch (Exception e) {
                throw new RuntimeException("数据库建表失败：" + e.getMessage());
            }

            // 3. 独立执行初始化数据 (DML)
            try {
                List<Long> ids = new ArrayList<>();
                for (int i = 1; i <= 60; i++) ids.add((long) i);
                String username = (String) session.getAttribute("username");
                searchMapper.insertInitialIds(tableName, ids, username);
            } catch (Exception e) {
                // 如果数据初始化失败，但表已经创建（DDL无法回滚），需要提示用户
                model.addAttribute("error", "表创建成功，但初始化 1-60 行数据失败：" + e.getMessage());
                session.setAttribute(TABLE_SESSION_KEY, tableName);
                return index(null, model, session);
            }
            
            session.setAttribute(TABLE_SESSION_KEY, tableName);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "操作失败：" + e.getMessage());
            model.addAttribute("showCreateModal", true);
            return index(null, model, session);
        }
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
            model.addAttribute("error", "系统权限表禁止删除！");
            return index(null, model, session);
        }

        String role = (String) session.getAttribute(AuthUtil.SESSION_ROLE);
        String username = (String) session.getAttribute("username");

        if ("teacher".equals(role)) {
            try {
                Map<String, Object> authInfo = tableAuthMapper.getTableAuthInfo(tableName);
                if (authInfo == null || !username.equals(authInfo.get("create_user"))) {
                    model.addAttribute("error", "您无权删除他人创建的表！");
                    return index(null, model, session);
                }
            } catch (Exception e) {
                model.addAttribute("error", "删除失败：无法校验表权限。");
                return index(null, model, session);
            }
        } else if ("student".equals(role)) {
            model.addAttribute("error", "学生无权删除表！");
            return index(null, model, session);
        }

        searchMapper.dropTable(tableName);
        String currentTableInSession = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (tableName.equals(currentTableInSession)) {
            session.removeAttribute(TABLE_SESSION_KEY);
        }
        return "redirect:/";
    }

    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user, Model model, HttpSession session) {
        if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "学生无权修改数据！");
            return index(null, model, session);
        }
        
        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        if (!hasWriteAuth(currentTable, session)) {
            model.addAttribute("error", "您无权操作该表的数据！");
            return index(null, model, session);
        }

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
        if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "学生无权修改数据！");
            return index(null, model, session);
        }

        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        if (!hasWriteAuth(currentTable, session)) {
            model.addAttribute("error", "您无权操作该表的数据！");
            return index(null, model, session);
        }

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
    public String deleteUser(@RequestParam("id") Long id, Model model, HttpSession session) {
        if (AuthUtil.isStudent(session)) {
            model.addAttribute("error", "学生无权删除数据！");
            return index(null, model, session);
        }

        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null) return "redirect:/";

        if (!hasWriteAuth(currentTable, session)) {
            model.addAttribute("error", "您无权操作该表的数据！");
            return index(null, model, session);
        }

        User emptyUser = new User();
        emptyUser.setId(id);
        emptyUser.setName(null);
        emptyUser.setAge(null);
        mappers.update(currentTable, emptyUser);
        return "redirect:/";
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

