package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysUser;
import com.mall.system.service.ISysPostService;
import com.mall.system.service.ISysRoleService;
import com.mall.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class SysUserApi extends BaseController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysUser>> getUserList(@RequestBody SysUser user) {
        List<SysUser> users = userService.selectUserList(user);
        return R.ok(users);
    }

    @GetMapping("/{userId}")
    @InnerAuth
    public R<SysUser> getUserById(@PathVariable("userId") Long userId) {
        SysUser user = userService.selectUserById(userId);
        return R.ok(user);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addUser(@RequestBody SysUser user) {
        return R.ok(userService.insertUser(user) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateUser(@RequestBody SysUser user) {
        return R.ok(userService.updateUser(user) > 0);
    }

    @DeleteMapping("/{userId}")
    @InnerAuth
    public R<Boolean> deleteUser(@PathVariable("userId") Long userId) {
        return R.ok(userService.deleteUserById(userId) > 0);
    }

    @PostMapping("/authRole")
    @InnerAuth
    public R<Boolean> authRole(Long userId, Long[] roleIds) {
        userService.insertUserAuth(userId, roleIds);
        return R.ok(true);
    }

    @GetMapping("/authRole/{userId}")
    @InnerAuth
    public R<List<Long>> getUserRoleIds(@PathVariable("userId") Long userId) {
        List<Long> roleIds = roleService.selectRoleListByUserId(userId);
        return R.ok(roleIds);
    }

    @GetMapping("/authPost/{userId}")
    @InnerAuth
    public R<List<Long>> getUserPostIds(@PathVariable("userId") Long userId) {
        List<Long> postIds = postService.selectPostListByUserId(userId);
        return R.ok(postIds);
    }

}