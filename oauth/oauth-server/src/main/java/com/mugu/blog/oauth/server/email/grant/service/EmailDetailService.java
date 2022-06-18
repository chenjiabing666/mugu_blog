package com.mugu.blog.oauth.server.email.grant.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 邮箱的查询业务接口
 */
public interface EmailDetailService {

    /**
     * 通过邮箱查找用户信息
     * @param email 邮箱
     * @return
     * @throws UsernameNotFoundException
     */
    UserDetails loadUserByEmail(String email) throws UsernameNotFoundException;
}
