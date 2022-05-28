package com.mugu.blog.common.annotation;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.mugu.blog.common.exception.ServiceException;
import com.mugu.blog.common.interceptor.SecurityContextHolder;
import com.mugu.blog.common.utils.OauthUtils;
import com.mugu.blog.core.model.LoginVal;
import com.mugu.blog.core.model.ResultCode;
import com.mugu.blog.core.utils.RequestContextUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;


/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description @RequiresLogin，@RequiresPermissions，@RequiresRoles 注解的切面
 */
@Aspect
@Component
public class PreAuthorizeAspect {
    /**
     * 构建
     */
    public PreAuthorizeAspect() {
    }

    /**
     * 定义AOP签名 (切入所有使用鉴权注解的方法)
     */
    public static final String POINTCUT_SIGN = " @annotation(com.mugu.blog.common.annotation.RequiresLogin) || "
            + "@annotation(com.mugu.blog.common.annotation.RequiresPermissions) || "
            + "@annotation(com.mugu.blog.common.annotation.RequiresRoles)";

    /**
     * 声明AOP签名
     */
    @Pointcut(POINTCUT_SIGN)
    public void pointcut() {
    }

    /**
     * 环绕切入
     *
     * @param joinPoint 切面对象
     * @return 底层方法执行后的返回值
     * @throws Throwable 底层方法抛出的异常
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 注解鉴权
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        checkMethodAnnotation(signature.getMethod());
        try {
            // 执行原有逻辑
            Object obj = joinPoint.proceed();
            return obj;
        } catch (Throwable e) {
            throw e;
        }
    }

    /**
     * 对一个Method对象进行注解检查
     */
    public void checkMethodAnnotation(Method method) {
        // 校验 @RequiresLogin 注解
        RequiresLogin requiresLogin = method.getAnnotation(RequiresLogin.class);
        if (requiresLogin != null) {
            doCheckLogin();
        }

        // 校验 @RequiresRoles 注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        if (requiresRoles != null) {
            doCheckRole(requiresRoles);
        }

        // 校验 @RequiresPermissions 注解
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        if (requiresPermissions != null) {
            doCheckPermissions(requiresPermissions);
        }
    }


    /**
     * 校验有无登录
     */
    private void doCheckLogin() {
        LoginVal loginVal = SecurityContextHolder.get();
        if (Objects.isNull(loginVal))
            throw new ServiceException(ResultCode.INVALID_TOKEN.getCode(), ResultCode.INVALID_TOKEN.getMsg());
    }

    /**
     * 校验有无对应的角色
     */
    private void doCheckRole(RequiresRoles requiresRoles){
        String[] roles = requiresRoles.value();
        LoginVal loginVal = OauthUtils.getCurrentUser();

        //该登录用户对应的角色
        String[] authorities = loginVal.getAuthorities();
        boolean match=false;

        //and 逻辑
        if (requiresRoles.logical()==Logical.AND){
            match = Arrays.stream(authorities).filter(StrUtil::isNotBlank).allMatch(item -> CollectionUtil.contains(Arrays.asList(roles), item));
        }else{  //OR 逻辑
            match = Arrays.stream(authorities).filter(StrUtil::isNotBlank).anyMatch(item -> CollectionUtil.contains(Arrays.asList(roles), item));
        }

        if (!match)
            throw new ServiceException(ResultCode.NO_PERMISSION.getCode(), ResultCode.NO_PERMISSION.getMsg());
    }

    /**
     * TODO 自己实现，由于并未集成前端的菜单权限，根据业务需求自己实现
     */
    private void doCheckPermissions(RequiresPermissions requiresPermissions){

    }


}
