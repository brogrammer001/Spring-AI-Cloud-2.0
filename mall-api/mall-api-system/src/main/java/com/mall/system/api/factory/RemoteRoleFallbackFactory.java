package com.mall.system.api.factory;

import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteRoleService;
import com.mall.system.api.domain.SysRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteRoleFallbackFactory implements FallbackFactory<RemoteRoleService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteRoleFallbackFactory.class);

    @Override
    public RemoteRoleService create(Throwable throwable)
    {
        log.error("角色服务调用失败:{}", throwable.getMessage());
        return new RemoteRoleService()
        {
            @Override
            public R<List<SysRole>> getRoleList(SysRole role) {
                return R.fail("查询角色列表失败:" + throwable.getMessage());
            }

            @Override
            public R<SysRole> getRoleById(Long roleId) {
                return R.fail("查询角色信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addRole(SysRole role) {
                return R.fail("新增角色失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateRole(SysRole role) {
                return R.fail("修改角色失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteRole(Long roleId) {
                return R.fail("删除角色失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> authDataScope(SysRole role) {
                return R.fail("角色数据权限分配失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Long>> getRoleMenuIds(Long roleId) {
                return R.fail("获取角色菜单列表失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> authMenu(SysRole role) {
                return R.fail("角色菜单权限分配失败:" + throwable.getMessage());
            }
        };
    }
}