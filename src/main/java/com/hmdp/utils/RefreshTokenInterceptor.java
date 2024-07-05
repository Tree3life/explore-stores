package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisUtils.Constant.LOGIN_USER_TTL;


/**
 * <p>
 * token查询用户信息并将用户的信息 和 请求进行绑定，并不断刷新 token的存活时间
 * （只做token刷新，不做权限/认证校验，校验工作交给{@link AuthInterceptor}）
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/6/6 9:14 </p>
 */
@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {
    /**
     * 请求头中 保存 token 的key
     */
    private static final String TOKEN_KEY_HEADER = "Authorization";

    private RedisUtils redisUtils;

    public RefreshTokenInterceptor(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    /**
     * 根据用户的token获取用户信息，
     * 并不断刷新 token的存活时间
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.debug("校验token，保存用户信息到 threadlocal中{}", request.toString());
        // 1.获取请求头中的token
        String token = request.getHeader(TOKEN_KEY_HEADER);
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 2.基于TOKEN获取redis中的用户
        String key = RedisUtils.Constant.LOGIN_SAVEUSER_KEY + token;
        Map<String, Object> map = redisUtils.getCacheMap(key);
        //2.1map转用户对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);

        // 3.用户不存在时，直接放行，不做刷新操作
        if (ObjectUtils.isEmpty(userDTO)) {
            return true;
        }

        // 5.将查询到的 用户信息保存到 ThreadLocal 中去
        SessionHolder.saveUser(userDTO);
        // 6.刷新token有效期
        redisUtils.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 7.放行
        log.info("校验token 结束");
        return true;
    }


    /**
     * 请求执行完之后 从该请求所在的线程域中将 该用户的信息进行移除
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SessionHolder.removeUser();
    }
}