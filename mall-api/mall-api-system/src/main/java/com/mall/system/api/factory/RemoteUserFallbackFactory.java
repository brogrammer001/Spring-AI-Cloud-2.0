package com.mall.system.api.factory;

import java.util.Collections;
import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteUserService;
import com.mall.system.api.domain.SysUser;
import com.mall.system.api.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService()
        {
            @Override
            public R<LoginUser> getUserInfo(String username, String source)
            {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> registerUserInfo(SysUser sysUser, String source)
            {
                return R.fail("注册用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> recordUserLogin(SysUser sysUser, String source)
            {
                return R.fail("记录用户登录信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addUser(SysUser sysUser) {
                return R.fail("新增用户失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysUser>> getUserList(SysUser sysUser) {
                return R.fail("查询用户列表失败:" + throwable.getMessage());
            }

            @Override
            public R<SysUser> getUserById(Long userId) {
                return R.fail("查询用户信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addUserApi(SysUser sysUser) {
                return R.fail("新增用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateUser(SysUser sysUser) {
                return R.fail("修改用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteUser(Long userId) {
                return R.fail("删除用户失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> authRole(Long userId, Long[] roleIds) {
                return R.fail("用户授权角色失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Long>> getUserRoleIds(Long userId) {
                return R.fail("获取用户角色列表失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Long>> getUserPostIds(Long userId) {
                return R.fail("获取用户岗位列表失败:" + throwable.getMessage());
            }
        };
    }
}
