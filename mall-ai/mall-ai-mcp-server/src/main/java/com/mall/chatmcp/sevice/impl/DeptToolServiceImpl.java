package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysDeptBo;
import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.domain.SysDept;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class DeptToolServiceImpl implements BaseToolService {

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private Validator validator;

    @Tool(description = "部门数据的新增、修改、删除。参数包含 operationType(add/update/delete)和部门实体。 [JSON]")
    public AjaxResult deptCrud(SysDeptBo deptBo) {
        String operationType = deptBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleDeptAdd(deptBo);
                case "update" -> handleDeptUpdate(deptBo);
                case "delete" -> handleDeptDelete(deptBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handleDeptAdd(SysDeptBo deptBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(deptBo, "sysDeptBo");
        validator.validate(deptBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);

        if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            Long parentId = getDeptIdByName(deptBo.getParentName());
            if (parentId == null) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            sysDept.setParentId(parentId);
        } else {
            sysDept.setParentId(0L);
        }

        sysDept.setStatus("0");

        R<Boolean> result = remoteDeptService.addDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private Long getDeptIdByName(String deptName) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        R<List<SysDept>> result = remoteDeptService.getDeptList(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            return result.getData().get(0).getDeptId();
        }
        return null;
    }

    private AjaxResult handleDeptUpdate(SysDeptBo deptBo) {
        if (deptBo.getDeptId() == null) {
            return AjaxResult.error("修改操作必须传入部门ID");
        }
        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);

        if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            Long parentId = getDeptIdByName(deptBo.getParentName());
            if (parentId == null) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            sysDept.setParentId(parentId);
        }

        R<Boolean> result = remoteDeptService.updateDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDeptDelete(SysDeptBo deptBo) {
        if (deptBo.getDeptId() == null) {
            return AjaxResult.error("删除操作必须传入部门ID");
        }
        R<Boolean> result = remoteDeptService.deleteDept(deptBo.getDeptId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

}