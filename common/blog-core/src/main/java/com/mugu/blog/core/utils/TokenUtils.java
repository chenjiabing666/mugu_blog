package com.mugu.blog.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mugu.blog.core.model.LoginVal;
import com.mugu.blog.core.model.oauth.OAuthConstant;

/**
 * @author 公众号：码猿技术专栏
 * @url: www.java-family.cn
 * @description 解析token信息
 */
public class TokenUtils {

    /**
     * 将JSON信息转换成LoginVal
     */
    public static LoginVal parseJsonToLoginVal(String json){
        JSONObject jsonObject = JSON.parseObject(json);
        //获取用户身份信息、权限信息
        String principal = jsonObject.getString(OAuthConstant.PRINCIPAL_NAME);
        String userId=jsonObject.getString(OAuthConstant.USER_ID);
        String jti = jsonObject.getString(OAuthConstant.JTI);
        Long expireIn = jsonObject.getLong(OAuthConstant.EXPR);
        String nickName = jsonObject.getString(OAuthConstant.NICK_NAME);
        Integer gender = jsonObject.getInteger(OAuthConstant.GENDER);
        String email = jsonObject.getString(OAuthConstant.EMAIL);
        String mobile = jsonObject.getString(OAuthConstant.MOBILE);
        String avatar = jsonObject.getString(OAuthConstant.AVATAR);
        JSONArray tempJsonArray = jsonObject.getJSONArray(OAuthConstant.AUTHORITIES_NAME);
        //权限
        String[] authorities =  tempJsonArray.toArray(new String[0]);

        //放入LoginVal
        LoginVal loginVal = new LoginVal();
        loginVal.setUserId(userId);
        loginVal.setUsername(principal);
        loginVal.setAuthorities(authorities);
        loginVal.setJti(jti);
        loginVal.setExpireIn(expireIn);
        loginVal.setNickname(nickName);
        loginVal.setEmail(email);
        loginVal.setAvatar(avatar);
        loginVal.setGender(gender);
        loginVal.setMobile(mobile);
        return loginVal;
    }
}
