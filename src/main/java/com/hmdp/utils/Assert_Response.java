package com.hmdp.utils;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;

import java.util.Map;
import java.util.function.Supplier;

/**
 * <p>
 * 校验并响应
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/6/28 18:20 </p>
 */
public class Assert_Response {
    private static final String TEMPLATE_VALUE_MUST_BE_BETWEEN_AND = "The value must be between {} and {}.";


    public static Result isTrue(boolean expression, String errorMsgTemplate) throws IllegalArgumentException {
        if (!expression) {
            return Result.fail(errorMsgTemplate);
        } else return Result.ok(true);
    }
}
