package com.mugu.blog.user.api.feign.fallback;

import com.mugu.blog.core.model.ResultCode;
import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.user.api.feign.UserFeign;
import com.mugu.blog.user.common.po.SysPermission;
import com.mugu.blog.user.common.po.SysRole;
import com.mugu.blog.user.common.po.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class UserFeignFallback implements UserFeign {

    @Override
    public ResultMsg<SysUser> getByUsername(String username) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }

    @Override
    public ResultMsg<List<SysRole>> getRolesByUserId(Long userId) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }

    @Override
    public ResultMsg<SysUser> getByUserId(String userId) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }

    @Override
    public ResultMsg<List<SysUser>> listByUserId(List<String> userIds) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }

    @Override
    public ResultMsg<List<SysPermission>> getPermissionByUserId(Long userId) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }

    @Override
    public ResultMsg<SysUser> getByEmail(String email) {
        log.error("User Server Fallback..........");
        return ResultMsg.resultFail(ResultCode.SERVER_FALLBACK.getCode(),ResultCode.SERVER_FALLBACK.getMsg());
    }
}
