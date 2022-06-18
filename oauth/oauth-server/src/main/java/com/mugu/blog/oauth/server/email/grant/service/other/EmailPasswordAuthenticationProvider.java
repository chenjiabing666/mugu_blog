package com.mugu.blog.oauth.server.email.grant.service.other;

import cn.hutool.core.util.StrUtil;
import com.mugu.blog.oauth.server.email.grant.service.EmailDetailService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description
 */
public class EmailPasswordAuthenticationProvider implements AuthenticationProvider {


    private EmailDetailService emailDetailService;

    private PasswordEncoder passwordEncoder;

    public EmailPasswordAuthenticationProvider(EmailDetailService emailDetailService,PasswordEncoder passwordEncoder){
        this.emailDetailService=emailDetailService;
        this.passwordEncoder=passwordEncoder;
    }

    /**
     * 执行认证处理的真正逻辑
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmailPasswordAuthenticationToken authenticationToken=(EmailPasswordAuthenticationToken)authentication;
        //email
        String email = (String)authentication.getPrincipal();
        String password =(String) authentication.getCredentials();
        UserDetails userDetails = emailDetailService.loadUserByEmail(email);

        if (Objects.isNull(userDetails)||!passwordEncoder.matches(password,userDetails.getPassword()))
            throw new BadCredentialsException("邮箱或者密码错误！");
        EmailPasswordAuthenticationToken authenticationResult=new EmailPasswordAuthenticationToken(userDetails,password,userDetails.getAuthorities());
        authenticationResult.setDetails(authenticationToken.getDetails());
        return authenticationResult;
    }

    /**
     * 判断是否支持此种授权类型认证
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return EmailPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
