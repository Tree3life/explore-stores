package com.hmdp.utils;


import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 认证拦截器,校验token，不断刷新 token的存活时间
 * 通过手动配置,放行 部分不必要的 接口
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/6/6 9:14 </p>
 */
public class AuthInterceptor implements HandlerInterceptor {
    /**
     * 对 需要 认证授权的接口 进行认证
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1.判断是否需要拦截（ThreadLocal中是否有用户）
        if (SessionHolder.getUser() == null) {
            // 没有用户，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
        return true;
    }

}
