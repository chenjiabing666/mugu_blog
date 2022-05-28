package com.mugu.blog.core.model;

import lombok.Data;

import java.util.List;

/**
 * 保存登录用户的信息，此处可以根据业务需要扩展
 */
@Data
public class LoginVal{

    private String userId;

    private String username;

    private String[] authorities;

    private String nickname;

    private Integer gender;

    private String avatar;

    private String mobile;

    private String email;

    /**
     * JWT令牌唯一ID
     */
    private String jti;

    /**
     * 过期时间，单位秒，距离过期时间还有多少秒
     */
    private Long expireIn;

    private List<String> permissions;
}
