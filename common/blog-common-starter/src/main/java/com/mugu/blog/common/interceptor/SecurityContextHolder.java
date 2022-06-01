package com.mugu.blog.common.interceptor;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.mugu.blog.core.model.LoginVal;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 使用TransmittableThreadLocal存储用户身份信息LoginVal
 */
public class SecurityContextHolder {

    //TTL
    private static final TransmittableThreadLocal<LoginVal> THREAD_LOCAL = new TransmittableThreadLocal<>();

    public static void set(LoginVal loginVal){
        THREAD_LOCAL.set(loginVal);
    }

    public static LoginVal get(){
        return THREAD_LOCAL.get();
    }

    public static void remove(){
        THREAD_LOCAL.remove();
    }

}
