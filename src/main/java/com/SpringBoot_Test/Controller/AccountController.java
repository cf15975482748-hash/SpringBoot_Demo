package com.SpringBoot_Test.Controller;

import com.SpringBoot_Test.Mapper.AccountMapper;
import com.SpringBoot_Test.Mapper.TableAuthMapper;
import com.SpringBoot_Test.Model.Admin;
import com.SpringBoot_Test.Model.Student;
import com.SpringBoot_Test.Model.Teacher;
import com.SpringBoot_Test.Util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
public class AccountController {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private TableAuthMapper tableAuthMapper;

    // --- 登录相关 ---

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam String username, @RequestParam String password, @RequestParam String role,
                          HttpSession session, RedirectAttributes ra) {
        Object user = "admin".equals(role) ? accountMapper.findAdminByUsername(username) :
                     "teacher".equals(role) ? accountMapper.findTeacherByUsername(username) :
                     accountMapper.findStudentByUsername(username);

        boolean success = false;
        if (user instanceof Admin && ((Admin) user).getPassword().equals(password)) success = true;
        else if (user instanceof Teacher && ((Teacher) user).getPassword().equals(password)) success = true;
        else if (user instanceof Student && ((Student) user).getPassword().equals(password)) success = true;

        if (success) {
            session.setAttribute(AuthUtil.SESSION_USER, user);
            session.setAttribute(AuthUtil.SESSION_ROLE, role);
            session.setAttribute("username", username);
            return "redirect:/";
        }
        ra.addFlashAttribute("error", "账号或密码错误");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- 账号管理 (Admin 专属) ---

    @GetMapping("/account-manage")
    public String accountManage(Model model, HttpSession session) {
        if (!AuthUtil.isAdmin(session)) return "redirect:/";
        model.addAttribute("admins", accountMapper.findAllAdmins());
        model.addAttribute("teachers", accountMapper.findAllTeachers());
        model.addAttribute("students", accountMapper.findAllStudents());
        return "account-manage";
    }

    @PostMapping("/admin/addAccount")
    public String addAccount(@RequestParam String role, @RequestParam String username, 
                             @RequestParam String password, @RequestParam String realName,
                             HttpSession session, RedirectAttributes ra) {
        if (!AuthUtil.isAdmin(session)) return "redirect:/";
        try {
            if ("admin".equals(role)) {
                Admin a = new Admin(); a.setUsername(username); a.setPassword(password); a.setRealName(realName);
                accountMapper.addAdmin(a);
            } else if ("teacher".equals(role)) {
                Teacher t = new Teacher(); t.setUsername(username); t.setPassword(password); t.setRealName(realName);
                accountMapper.addTeacher(t);
            } else {
                Student s = new Student(); s.setUsername(username); s.setPassword(password); s.setRealName(realName);
                accountMapper.addStudent(s);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "账号已存在或操作失败");
        }
        return "redirect:/account-manage";
    }

    @PostMapping("/admin/deleteAccount")
    public String deleteAccount(@RequestParam String role, @RequestParam String username, HttpSession session) {
        if (!AuthUtil.isAdmin(session)) return "redirect:/";
        if ("admin".equals(role)) accountMapper.deleteAdmin(username);
        else if ("teacher".equals(role)) accountMapper.deleteTeacher(username);
        else accountMapper.deleteStudent(username);
        return "redirect:/account-manage";
    }

    // --- 权限配置 (Teacher 专属) ---

    @PostMapping("/teacher/updateWhitelist")
    public String updateWhitelist(@RequestParam String tableName, @RequestParam String studentList, HttpSession session) {
        if (!AuthUtil.isTeacher(session)) return "redirect:/";
        // 这里简单逗号分隔保存
        tableAuthMapper.updateTableWhitelist(tableName, studentList);
        return "redirect:/";
    }

    // --- 密码修改 (Student 专属) ---
    @PostMapping("/student/updatePassword")
    public String updatePassword(@RequestParam String newPassword, HttpSession session) {
        if (!AuthUtil.isStudent(session)) return "redirect:/";
        String username = AuthUtil.getCurrentUsername(session);
        Student s = accountMapper.findStudentByUsername(username);
        s.setPassword(newPassword);
        accountMapper.updateStudent(s);
        return "redirect:/logout";
    }
}
