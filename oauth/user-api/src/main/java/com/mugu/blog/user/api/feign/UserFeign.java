package com.mugu.blog.user.api.feign;

import com.mugu.blog.core.model.ResultMsg;
import com.mugu.blog.user.api.feign.fallback.UserFeignFallback;
import com.mugu.blog.user.common.po.SysPermission;
import com.mugu.blog.user.common.po.SysRole;
import com.mugu.blog.user.common.po.SysUser;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * user的feign接口
 */
@FeignClient(value = "blog-user-boot",contextId = "user-boot",fallback = UserFeignFallback.class)
public interface UserFeign {

    @GetMapping("/blog-user-boot/user/getByUsername")
    ResultMsg<SysUser> getByUsername(@RequestParam(value = "username") String username);

    @GetMapping("/blog-user-boot/user/getRolesByUserId")
    ResultMsg<List<SysRole>> getRolesByUserId(@RequestParam(value = "userId") Long userId);

    @GetMapping("/blog-user-boot/user/getByUserId")
    ResultMsg<SysUser> getByUserId(@RequestParam(value = "userId") String userId);

    @GetMapping("/blog-user-boot/user/listByUserId")
    ResultMsg<List<SysUser>> listByUserId(@RequestBody List<String> userIds);

    @GetMapping("/blog-user-boot/user/getPermissionByUserId")
    ResultMsg<List<SysPermission>> getPermissionByUserId(@RequestParam(value = "userId") Long userId);

}
