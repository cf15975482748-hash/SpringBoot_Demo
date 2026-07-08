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

    public static boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(SESSION_USER) != null;
    }

    public static String getCurrentUsername(HttpSession session) {
        return (String) session.getAttribute("username");
    }
}
