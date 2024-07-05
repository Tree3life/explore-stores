package com.hmdp.config;

import com.hmdp.utils.AuthInterceptor;
import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RedisUtils;
import com.hmdp.utils.RefreshTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * <p>
 *
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/6/28 21:06 </p>
 */
@Slf4j
@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

    @Resource
    private RedisUtils redisUtils;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.debug(" 注册AuthInterceptor拦截器");
        AuthInterceptor authInterceptor = new AuthInterceptor();
        registry.addInterceptor(new RefreshTokenInterceptor(redisUtils)).addPathPatterns(
                "/**"
        ).order(1);
        registry.addInterceptor(authInterceptor)
                //方向不必要的拦截
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                )
                //要位于 RefreshTokenInterceptor 拦截器之后
                .order(2);
    }

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        log.debug(" 注册loginInterceptorr拦截器");
//        LoginInterceptor loginInterceptor = new LoginInterceptor();
////        registry.addInterceptor(new RefreshTokenInterceptor(redisUtils)).addPathPatterns(
////                "/**"
////        ).order(1);
//        registry.addInterceptor(loginInterceptor)
//                //方向不必要的拦截
//                .excludePathPatterns(
//                        "/shop/**",
//                        "/voucher/**",
//                        "/shop-type/**",
//                        "/upload/**",
//                        "/blog/hot",
//                        "/user/code",
//                        "/user/login"
//                );
//    }
}
