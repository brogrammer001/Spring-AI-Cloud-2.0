package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysDept;
import com.mall.system.service.ISysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
public class SysDeptApi extends BaseController {

    @Autowired
    private ISysDeptService deptService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysDept>> getDeptList(@RequestBody SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return R.ok(depts);
    }

    @GetMapping("/{deptId}")
    @InnerAuth
    public R<SysDept> getDeptById(@PathVariable("deptId") Long deptId) {
        SysDept dept = deptService.selectDeptById(deptId);
        return R.ok(dept);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addDept(@RequestBody SysDept dept) {
        return R.ok(deptService.insertDept(dept) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateDept(@RequestBody SysDept dept) {
        return R.ok(deptService.updateDept(dept) > 0);
    }

    @DeleteMapping("/{deptId}")
    @InnerAuth
    public R<Boolean> deleteDept(@PathVariable("deptId") Long deptId) {
        return R.ok(deptService.deleteDeptById(deptId) > 0);
    }

}