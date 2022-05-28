package com.mugu.blog.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 登录认证的注解，标注在controller方法上，一定要是登录才能的访问的接口
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresLogin {
}
