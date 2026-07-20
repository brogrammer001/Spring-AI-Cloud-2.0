package com.mall.system.api;

import com.mall.common.core.constant.SecurityConstants;
import com.mall.common.core.constant.ServiceNameConstants;
import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysUser;
import com.mall.system.api.factory.RemoteUserFallbackFactory;
import com.mall.system.api.model.LoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务
 * 
 * @author mall
 */
@FeignClient(contextId = "remoteUserService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService
{
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/user/info/{username}")
    public R<LoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/user/register")
    public R<Boolean> registerUserInfo(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 记录用户登录IP地址和登录时间
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/user/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @return 结果
     */
    @PostMapping("/user/addUser")
    public R<Boolean> addUser(@RequestBody SysUser sysUser);

    @PostMapping("/api/user/list")
    public R<List<SysUser>> getUserList(@RequestBody SysUser sysUser);

    @GetMapping("/api/user/{userId}")
    public R<SysUser> getUserById(@PathVariable("userId") Long userId);

    @PostMapping("/api/user/add")
    public R<Boolean> addUserApi(@RequestBody SysUser sysUser);

    @PutMapping("/api/user/update")
    public R<Boolean> updateUser(@RequestBody SysUser sysUser);

    @DeleteMapping("/api/user/{userId}")
    public R<Boolean> deleteUser(@PathVariable("userId") Long userId);

    @PostMapping("/api/user/authRole")
    public R<Boolean> authRole(@RequestParam("userId") Long userId, @RequestParam("roleIds") Long[] roleIds);

    @GetMapping("/api/user/authRole/{userId}")
    public R<List<Long>> getUserRoleIds(@PathVariable("userId") Long userId);

    @GetMapping("/api/user/authPost/{userId}")
    public R<List<Long>> getUserPostIds(@PathVariable("userId") Long userId);

}
