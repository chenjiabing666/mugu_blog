package com.mugu.blog.core.model.oauth;

import com.mugu.blog.core.constant.KeyConstant;

public class OAuthConstant {
    public final static String TOKEN_NAME="jwt-token";

    public final static String PRINCIPAL_NAME="principal";

    public static final String AUTHORITIES_NAME="authorities";

    public static final String USER_ID="user_id";
    public static final String GENDER="gender";
    public static final String AVATAR="avatar";
    public static final String MOBILE="mobile";
    public static final String EMAIL="email";
    public static final String NICK_NAME="nick_name";
    public static final String PERMISSION="permissions";


    public static final String JTI="jti";

    public static final String EXPR="expr";

    /**
     * 权限<->url对应的KEY
     */
    public final static String OAUTH_URLS= KeyConstant.REDIS_KEY_PREFIX+"oauth2:oauth_urls";

    /**
     * JWT令牌黑名单的KEY
     */
    public final static String JTI_KEY_PREFIX=KeyConstant.REDIS_KEY_PREFIX+"oauth2:black:";

    /**
     * 角色前缀
     */
    public final static String ROLE_PREFIX="ROLE_";

    public final static String METHOD_SUFFIX=":";

    public final static String ROLE_ROOT_CODE=ROLE_PREFIX+"ROOT";

    public final static String ROLE_ADMIN_CODE=ROLE_PREFIX+"ADMIN";

}
