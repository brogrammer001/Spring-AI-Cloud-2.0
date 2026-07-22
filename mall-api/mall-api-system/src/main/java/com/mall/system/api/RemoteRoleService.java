package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysRole;
import com.mall.system.api.factory.RemoteRoleFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remoteRoleService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteRoleFallbackFactory.class)
public interface RemoteRoleService {

    @PostMapping("/api/role/list")
    public R<List<SysRole>> getRoleList(@RequestBody SysRole role);

    @GetMapping("/api/role/{roleId}")
    public R<SysRole> getRoleById(@PathVariable("roleId") Long roleId);

    @PostMapping("/api/role/add")
    public R<Boolean> addRole(@RequestBody SysRole role);

    @PutMapping("/api/role/update")
    public R<Boolean> updateRole(@RequestBody SysRole role);

    @DeleteMapping("/api/role/{roleId}")
    public R<Boolean> deleteRole(@PathVariable("roleId") Long roleId);

    @PutMapping("/api/role/authDataScope")
    public R<Boolean> authDataScope(@RequestBody SysRole role);

    @GetMapping("/api/role/authMenu/{roleId}")
    public R<List<Long>> getRoleMenuIds(@PathVariable("roleId") Long roleId);

    @PutMapping("/api/role/authMenu")
    public R<Boolean> authMenu(@RequestBody SysRole role);

}