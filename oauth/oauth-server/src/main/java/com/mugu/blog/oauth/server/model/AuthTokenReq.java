package com.mugu.blog.oauth.server.model;

import lombok.Data;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description
 */
@Data
public class AuthTokenReq {
    //授权类型
    private String grantType;

    //客户端id
    private String clientId;

    //客户端秘钥
    private String clientSecret;

    //邮箱验证专用
    private String email;

    private String username;

    private String password;
}
