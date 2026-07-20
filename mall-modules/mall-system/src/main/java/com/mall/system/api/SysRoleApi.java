package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysRole;
import com.mall.system.service.ISysMenuService;
import com.mall.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class SysRoleApi extends BaseController {

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysRole>> getRoleList(@RequestBody SysRole role) {
        List<SysRole> roles = roleService.selectRoleList(role);
        return R.ok(roles);
    }

    @GetMapping("/{roleId}")
    @InnerAuth
    public R<SysRole> getRoleById(@PathVariable("roleId") Long roleId) {
        SysRole role = roleService.selectRoleById(roleId);
        return R.ok(role);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addRole(@RequestBody SysRole role) {
        return R.ok(roleService.insertRole(role) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateRole(@RequestBody SysRole role) {
        return R.ok(roleService.updateRole(role) > 0);
    }

    @DeleteMapping("/{roleId}")
    @InnerAuth
    public R<Boolean> deleteRole(@PathVariable("roleId") Long roleId) {
        return R.ok(roleService.deleteRoleById(roleId) > 0);
    }

    @PutMapping("/authDataScope")
    @InnerAuth
    public R<Boolean> authDataScope(@RequestBody SysRole role) {
        return R.ok(roleService.authDataScope(role) > 0);
    }

    @GetMapping("/authMenu/{roleId}")
    @InnerAuth
    public R<List<Long>> getRoleMenuIds(@PathVariable("roleId") Long roleId) {
        List<Long> menuIds = menuService.selectMenuListByRoleId(roleId);
        return R.ok(menuIds);
    }

}