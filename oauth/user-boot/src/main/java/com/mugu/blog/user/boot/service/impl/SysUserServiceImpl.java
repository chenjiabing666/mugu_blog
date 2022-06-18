package com.mugu.blog.user.boot.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.mugu.blog.user.boot.dao.SysUserMapper;
import com.mugu.blog.user.boot.service.SysUserService;
import com.mugu.blog.user.common.po.SysPermission;
import com.mugu.blog.user.common.po.SysRole;
import com.mugu.blog.user.common.po.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectByUserName(username);
    }

    @Override
    public SysUser getUserByEmail(String email) {
        return sysUserMapper.selectByEmail(email);
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return sysUserMapper.selectRolesByUserId(userId);
    }

    @Override
    public SysUser getByUserId(String userId) {
        return sysUserMapper.selectByUserId(userId);
    }

    @Override
    public List<SysUser> listByUserId(List<String> userIds) {
        if (CollectionUtil.isEmpty(userIds))
            return null;
        return sysUserMapper.listByUserId(userIds);
    }

    @Override
    public List<SysPermission> getPermissionByUserId(Long userId) {
        return sysUserMapper.listPermissionByUserId(userId);
    }
}

