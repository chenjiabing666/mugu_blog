package com.mugu.blog.common.annotation;

import com.mugu.blog.core.model.oauth.OAuthConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 标注在controller方法上，确保拥有指定的角色才能访问该接口
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRoles {
    /**
     * 需要校验的角色标识，默认超管和管理员
     */
    String[] value() default {OAuthConstant.ROLE_ROOT_CODE,OAuthConstant.ROLE_ADMIN_CODE};

    /**
     * 验证逻辑：AND | OR，默认AND
     */
    Logical logical() default Logical.AND;
}
