package com.mugu.blog.user.boot.controller;

import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.user.boot.service.SysUserService;
import com.mugu.blog.user.common.po.SysPermission;
import com.mugu.blog.user.common.po.SysRole;
import com.mugu.blog.user.common.po.SysUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(value = "OAuth所需的接口")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    public SysUserService sysUserService;

    @ApiOperation("根据用户名查询详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", defaultValue = "user", value = "用户名", paramType = "query", dataType = "String"),
    })
    @GetMapping("/getByUsername")
    public ResultMsg<SysUser> getByUsername(String username){
        return ResultMsg.resultSuccess(sysUserService.getUserByUsername(username));
    }

    @ApiOperation("根据邮箱查询详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", defaultValue = "123@qq.com", value = "邮箱", paramType = "query", dataType = "String"),
    })
    @GetMapping("/getByEmail")
    public ResultMsg<SysUser> getByEmail(String email){
        return ResultMsg.resultSuccess(sysUserService.getUserByEmail(email));
    }


    @ApiOperation("根据用户唯一ID查询角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", defaultValue = "user", value = "用户唯一ID", paramType = "query", dataType = "String"),
    })
    @GetMapping("/getRolesByUserId")
    public ResultMsg<List<SysRole>> getRolesByUserId(Long userId){
        return ResultMsg.resultSuccess(sysUserService.getRolesByUserId(userId));
    }

    @ApiOperation("根据用户ID查询用户详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", defaultValue = "user", value = "用户唯一ID", paramType = "query", dataType = "String"),
    })
    @GetMapping("/getByUserId")
    public ResultMsg<SysUser> getByUserId(String userId){
        return ResultMsg.resultSuccess(sysUserService.getByUserId(userId));
    }

    @ApiOperation("根据用户ID批量查询")
    @GetMapping("/listByUserId")
    public ResultMsg<List<SysUser>> listByUserId(@RequestBody List<String> userIds){
        return ResultMsg.resultSuccess(sysUserService.listByUserId(userIds));
    }

    @ApiOperation("根据用户唯一ID查询权限列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", defaultValue = "user", value = "用户唯一ID", paramType = "query", dataType = "String"),
    })
    @GetMapping("/getPermissionByUserId")
    public ResultMsg<List<SysPermission>> getPermissionByUserId(Long userId){
        return ResultMsg.resultSuccess(sysUserService.getPermissionByUserId(userId));
    }

}
