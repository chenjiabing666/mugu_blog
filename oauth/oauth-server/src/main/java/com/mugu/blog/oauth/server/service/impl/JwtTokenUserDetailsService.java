package com.mugu.blog.oauth.server.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.core.model.oauth.OAuthConstant;
import com.mugu.blog.oauth.server.model.SecurityUser;
import com.mugu.blog.user.api.feign.UserFeign;
import com.mugu.blog.user.common.po.SysRole;
import com.mugu.blog.user.common.po.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * UserDetailsService的实现类，从数据库加载用户的信息，比如密码、角色。。。。
 */
@Service
public class JwtTokenUserDetailsService implements UserDetailsService {

    @Autowired
    private UserFeign userFeign;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ResultMsg<SysUser> sysUserResult = userFeign.getByUsername(username);
        SysUser user = sysUserResult.getData();
        if (!ResultMsg.isSuccess(sysUserResult)||Objects.isNull(sysUserResult.getData())){
            throw new UsernameNotFoundException("用户不存在！");
        }

        //获取当前用户的角色
        ResultMsg<List<SysRole>> roleResult = userFeign.getRolesByUserId(user.getId());
        if (!ResultMsg.isSuccess(roleResult)||Objects.isNull(roleResult.getData())){
            throw new UsernameNotFoundException("获取角色失败！");
        }

        List<String> roles = Objects.requireNonNull(roleResult.getData()).stream().map(sysRole -> OAuthConstant.ROLE_PREFIX+sysRole.getCode()).collect(Collectors.toList());

        return SecurityUser.builder()
                .nickname(user.getNickname())
                .gender(user.getGender())
                .avatar(user.getAvatar())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .userId(user.getUserId())
                .username(user.getUsername())
                .password(user.getPassword())
                //将角色放入authorities中
                .authorities(AuthorityUtils.createAuthorityList(ArrayUtil.toArray(roles,String.class)))
                .build();
    }
}
