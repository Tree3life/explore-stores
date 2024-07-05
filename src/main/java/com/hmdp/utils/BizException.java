package com.hmdp.utils;

/**
 * <p>
 *
 * </p>
 * <a>@Author: Rupert</ a>
 * <p>创建时间: 2024/7/3 12:15 </p>
 */
public class BizException extends RuntimeException{
    public BizException() {
    }

    public BizException(String message) {
        super(message);
    }
}
