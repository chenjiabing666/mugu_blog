package com.mugu.blog.common.interceptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mugu.blog.core.model.LoginVal;
import com.mugu.blog.core.model.oauth.OAuthConstant;
import com.mugu.blog.core.utils.TokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 拦截器，在preHandle中解析请求头的中的token信息，将其放入SecurityContextHolder中
 *                      在afterCompletion方法中移除对应的ThreadLocal中信息
 *                      确保每个请求的用户信息独立
 */
@Component
public class AuthInterceptor implements AsyncHandlerInterceptor {


    /**
     * 在执行controller方法之前将请求头中的token信息解析出来，放入SecurityContextHolder中（ThreadLocal）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod))
            return true;
        //获取请求头中的加密的用户信息
        String token = request.getHeader(OAuthConstant.TOKEN_NAME);
        if (StrUtil.isBlank(token))
            return true;
        //解密
        String json = Base64.decodeStr(token);
        //将json解析成LoginVal
        LoginVal loginVal = TokenUtils.parseJsonToLoginVal(json);
        //封装数据到ThreadLocal中
        SecurityContextHolder.set(loginVal);
        return true;
    }

    /**
     * 将请求中的
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SecurityContextHolder.remove();
    }
}
