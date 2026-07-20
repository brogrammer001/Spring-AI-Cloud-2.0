package com.mall.system.api.factory;

import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteDeptFallbackFactory implements FallbackFactory<RemoteDeptService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteDeptFallbackFactory.class);

    @Override
    public RemoteDeptService create(Throwable throwable)
    {
        log.error("部门服务调用失败:{}", throwable.getMessage());
        return new RemoteDeptService()
        {
            @Override
            public R<List<SysDept>> getDeptList(SysDept dept) {
                return R.fail("查询部门列表失败:" + throwable.getMessage());
            }

            @Override
            public R<SysDept> getDeptById(Long deptId) {
                return R.fail("查询部门信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addDept(SysDept dept) {
                return R.fail("新增部门失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateDept(SysDept dept) {
                return R.fail("修改部门失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteDept(Long deptId) {
                return R.fail("删除部门失败:" + throwable.getMessage());
            }
        };
    }
}