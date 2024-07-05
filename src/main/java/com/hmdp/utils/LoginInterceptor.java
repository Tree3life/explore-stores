package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 登录拦截器
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/7/1 9:44 </p>
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
//        User user = (User) session.getAttribute("user");
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setNickName(user.getNickName());
        userDTO.setId(user.getId());
        userDTO.setIcon(user.getIcon());
        SessionHolder.saveUser(userDTO);
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SessionHolder.removeUser();
    }
}
