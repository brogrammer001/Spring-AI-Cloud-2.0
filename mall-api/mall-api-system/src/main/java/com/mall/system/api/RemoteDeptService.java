package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysDept;
import com.mall.system.api.factory.RemoteDeptFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remoteDeptService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteDeptFallbackFactory.class)
public interface RemoteDeptService {

    @PostMapping("/api/dept/list")
    public R<List<SysDept>> getDeptList(@RequestBody SysDept dept);

    @GetMapping("/api/dept/{deptId}")
    public R<SysDept> getDeptById(@PathVariable("deptId") Long deptId);

    @PostMapping("/api/dept/add")
    public R<Boolean> addDept(@RequestBody SysDept dept);

    @PutMapping("/api/dept/update")
    public R<Boolean> updateDept(@RequestBody SysDept dept);

    @DeleteMapping("/api/dept/{deptId}")
    public R<Boolean> deleteDept(@PathVariable("deptId") Long deptId);

}