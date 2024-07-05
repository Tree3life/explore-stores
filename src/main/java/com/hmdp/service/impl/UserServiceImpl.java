package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.Assert_Response;
import com.hmdp.utils.RedisUtils;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        boolean isPhone = RegexUtils.isPhoneInvalid(phone);
        Assert_Response.isTrue(isPhone, "手机号异常");

        //2.生成验证码
        String verifyCode = RandomUtil.randomNumbers(6);

        // 3.缓存验证码
//        session.setAttribute("code", verifyCode);
        redisUtils.setCacheObject(RedisUtils.Constant.LOGIN_SENDCODE_KEY + phone, verifyCode, 30, TimeUnit.MINUTES);

        // 4.发送验证码
        log.info("验证码为：{}", verifyCode);
        // 5.返回结果
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验验证码
//        String code = (String) session.getAttribute("code");
        String code = redisUtils.getCacheObject(RedisUtils.Constant.LOGIN_SENDCODE_KEY + loginForm.getPhone());
        Assert.notNull(loginForm, "登录信息不能为空");

        if (code == null || !code.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }

        // 2.根据用户查询手机号
        User dbUser = this.getOne(new QueryWrapper<>(new User().setPhone(loginForm.getPhone())));
//        User dbUser = query().eq("phone", loginForm.getPhone()).one();
        // 3.用户存在？"":创建新用户并保存到数据库中去
        if (dbUser == null) {
            //"用户不存在",进行注册
            dbUser = createUser(loginForm.getPhone());
            this.save(dbUser);
        }

        // 4.保存用户到session中去
//        session.setAttribute("user", dbUser);
        // 4.保存用户到redis中
        //4.1生成token
        String token = UUID.randomUUID().toString(true);
        // 4.2构建要保存的userDto对象
        UserDTO userDTO = BeanUtil.copyProperties(dbUser, UserDTO.class);
        // 4.2将对象以map形式保存到redis中
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor(((fieldName, fieldValue) -> {
                    return fieldValue.toString();
                })));

        String userKey = RedisUtils.Constant.LOGIN_SAVEUSER_KEY + token;
        redisUtils.setCacheMap(userKey, userMap);
        redisUtils.expire(userKey, 30, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("用户" + RandomUtil.randomString(9).toString());
        return user;
    }

}
