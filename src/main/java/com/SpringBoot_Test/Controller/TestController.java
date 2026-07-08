package com.SpringBoot_Test.Controller;


import com.SpringBoot_Test.Mapper.TableAuthMapper;
import com.SpringBoot_Test.Mapper.AccountMapper;
import com.SpringBoot_Test.Model.User;
import com.SpringBoot_Test.Model.Admin;
import com.SpringBoot_Test.Model.Teacher;
import com.SpringBoot_Test.Model.Student;
import com.SpringBoot_Test.Mapper.SearchMapper;
import com.SpringBoot_Test.Mapper.Mappers;
import com.SpringBoot_Test.Util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Autowired
    private AccountMapper accountMapper;

    private static final String TABLE_SESSION_KEY = "CURRENT_TABLE";

    @GetMapping("/")
    public String index(@RequestParam(value = "searchName", required = false) String searchName, Model model, HttpSession session) {
        List<String> visibleTables = new ArrayList<>();
        String role = (String) session.getAttribute(AuthUtil.SESSION_ROLE);
        String username = (String) session.getAttribute("username");
        
        for (String tableName : searchMapper.getAllTables()) {
            if ("admin".equals(role)) {
                visibleTables.add(tableName);
            } else if ("teacher".equals(role) || "student".equals(role)) {
                // 移除白名单过滤逻辑，老师和学生都能看到所有业务表
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
            return "index";
        }

        String currentTable = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (currentTable == null || !visibleTables.contains(currentTable)) {
            currentTable = visibleTables.get(0);
            session.setAttribute(TABLE_SESSION_KEY, currentTable);
        }

        // 数据加载逻辑适配
        List<User> displayUsers = new ArrayList<>();
        boolean hasSearch = searchName != null && !searchName.trim().isEmpty();
        String searchKey = hasSearch ? searchName.trim() : null;

        if (!isBusinessTable(currentTable)) {
            // 系统表数据适配 + 搜索支持 (显示真实姓名和密码)
            if ("admin".equals(currentTable)) {
                List<Admin> admins = hasSearch ? accountMapper.searchAdmins(searchKey) : accountMapper.findAllAdmins();
                admins.forEach(a -> displayUsers.add(mapToUser(a.getId(), a.getRealName(), a.getPassword())));
            } else if ("teacher".equals(currentTable)) {
                List<Teacher> teachers = hasSearch ? accountMapper.searchTeachers(searchKey) : accountMapper.findAllTeachers();
                teachers.forEach(t -> displayUsers.add(mapToUser(t.getId(), t.getRealName(), t.getPassword())));
            } else if ("student".equals(currentTable)) {
                List<Student> students = hasSearch ? accountMapper.searchStudents(searchKey) : accountMapper.findAllStudents();
                students.forEach(s -> displayUsers.add(mapToUser(s.getId(), s.getRealName(), s.getPassword())));
            }
        } else {
            // 业务表逻辑
            List<User> finalDisplayUsers = hasSearch ? mappers.searchByName(currentTable, searchKey) : searchMapper.findAll(currentTable);
            displayUsers.addAll(finalDisplayUsers);
        }
        
        if (hasSearch) model.addAttribute("searchName", searchName);
        model.addAttribute("users", displayUsers);
        model.addAttribute("currentTable", currentTable);
        return "index";
    }

    private User mapToUser(Long id, String name, String password) {
        User u = new User(); u.setId(id); u.setName(name); u.setPassword(password); return u;
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
            model.addAttribute("error", "权限不足！学生仅拥有只读权限。");
            return index(null, model, session);
        }
        String table = (String) session.getAttribute(TABLE_SESSION_KEY);
        if (table == null || !isBusinessTable(table) || !hasWriteAuth(table, session)) {
            model.addAttribute("error", "非法操作：系统账号表禁止在此处修改，或您无权操作该表！");
            return index(null, model, session);
        }

        // BUG2 修复：增加 ID 非空与存在性校验
        if (user.getId() == null) {
            model.addAttribute("error", "操作失败：ID 不能为空！");
            return index(null, model, session);
        }

        try {
            // 校验 ID 存在性
            if (searchMapper.countById(table, user.getId()) == 0) {
                model.addAttribute("error", "操作失败：该 ID [" + user.getId() + "] 数据不存在，无需删除或修改。");
                return index(null, model, session);
            }

            String error = validateUser(user);
            if (error != null) {
                model.addAttribute("error", error);
                if (modalFlag != null) model.addAttribute(modalFlag, true);
                if ("showEditDataModal".equals(modalFlag)) model.addAttribute("failedUser", user);
                return index(null, model, session);
            }

            // 如果是删除操作 (modalFlag 为 null 且 User 字段为空)
            if (modalFlag == null && user.getName() == null && user.getScore() == null) {
                mappers.delete(table, user.getId()); // 执行物理删除或按原逻辑置空
            } else {
                mappers.update(table, user);
            }
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "数据库操作失败：" + e.getMessage());
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
        // 成绩范围校验 (如果填写了则校验范围 0-100)
        if (user.getScore() != null && (user.getScore() < 0 || user.getScore() > 100)) {
            return "成绩必须在 0-100 之间！";
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

