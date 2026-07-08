package com.SpringBoot_Test.Util;

import javax.servlet.http.HttpSession;

public class AuthUtil {

    public static final String SESSION_USER = "loginUser";
    public static final String SESSION_ROLE = "loginRole";

    public static boolean isAdmin(HttpSession session) {
        return "admin".equals(session.getAttribute(SESSION_ROLE));
    }

    public static boolean isTeacher(HttpSession session) {
        return "teacher".equals(session.getAttribute(SESSION_ROLE));
    }

    public static boolean isStudent(HttpSession session) {
        return "student".equals(session.getAttribute(SESSION_ROLE));
    }

    public static String getCurrentUsername(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER);
        if (user == null) return null;
        // 假设 SessionUser 或者是具体实体类，这里简单获取用户名
        return (String) session.getAttribute("username");
    }
}
