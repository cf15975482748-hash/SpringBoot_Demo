package com.SpringBoot_Test.Interceptor;

import com.SpringBoot_Test.Util.AuthUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!AuthUtil.isLoggedIn(request.getSession())) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}
