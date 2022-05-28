package com.mugu.blog.common.utils;

import com.mugu.blog.common.interceptor.SecurityContextHolder;
import com.mugu.blog.core.model.LoginVal;
import com.mugu.blog.core.model.RequestConstant;
import com.mugu.blog.core.utils.RequestContextUtils;

import java.util.Objects;

/**
 * 从Request中获取用户身份信息
 */
public class OauthUtils {
    public static LoginVal getCurrentUser(){
        return Objects.requireNonNull(SecurityContextHolder.get());
    }
}
