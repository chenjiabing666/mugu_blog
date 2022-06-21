package com.mugu.blog.oauth.server.email.grant.service;

import com.mugu.blog.oauth.server.email.grant.service.EmailDetailService;
import com.mugu.blog.oauth.server.email.grant.service.other.EmailPasswordAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 邮箱授权类型的配置类
 */
@Configuration
public class EmailSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailDetailService emailDetailService;

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        EmailPasswordAuthenticationProvider provider = new EmailPasswordAuthenticationProvider(emailDetailService, passwordEncoder);
        builder.authenticationProvider(provider);
    }
}
