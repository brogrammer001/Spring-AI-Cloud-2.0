package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysDeptBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.domain.SysDept;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class DeptToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "部门数据的新增、修改、删除。参数包含 operationType(add/update/delete)和部门实体。")
    public AjaxResult deptCrud(SysDeptBo deptBo) {
        String operationType = deptBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleDeptAdd(deptBo);
            case "update" -> handleDeptUpdate(deptBo);
            case "delete" -> handleDeptDelete(deptBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "部门操作");
    }

    private AjaxResult handleDeptAdd(SysDeptBo deptBo) {
        AjaxResult validateResult = validate(deptBo, "sysDeptBo");
        if (validateResult != null) {
            return validateResult;
        }

        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);

        if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
            if (parentDepts.isEmpty()) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            if (parentDepts.size() > 1) {
                return AjaxResult.error(buildMultipleDeptMessage("查询到多个父部门，请补充更多信息后重试：", parentDepts));
            }
            sysDept.setParentId(parentDepts.getFirst().getDeptId());
        } else {
            sysDept.setParentId(0L);
        }

        sysDept.setStatus("0");

        R<Boolean> result = remoteDeptService.addDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private List<SysDept> getDeptsByName(String deptName) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        R<List<SysDept>> result = remoteDeptService.getDeptList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysDept> getDeptsByConditions(SysDeptBo deptBo) {
        SysDept query = new SysDept();
        if (deptBo.getDeptName() != null && !deptBo.getDeptName().isEmpty()) {
            query.setDeptName(deptBo.getDeptName());
        }
        if (deptBo.getLeader() != null && !deptBo.getLeader().isEmpty()) {
            query.setLeader(deptBo.getLeader());
        }
        if (deptBo.getPhone() != null && !deptBo.getPhone().isEmpty()) {
            query.setPhone(deptBo.getPhone());
        }
        R<List<SysDept>> result = remoteDeptService.getDeptList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private String buildMultipleDeptMessage(String prefix, List<SysDept> depts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (SysDept d : depts) {
            sb.append("部门名称：").append(d.getDeptName())
              .append("，负责人：").append(d.getLeader() != null ? d.getLeader() : "无")
              .append("，联系电话：").append(d.getPhone() != null ? d.getPhone() : "无").append("\n");
        }
        return sb.toString();
    }

    private AjaxResult handleDeptUpdate(SysDeptBo deptBo) {
        if (deptBo.getDeptName() == null || deptBo.getDeptName().isEmpty()) {
            return AjaxResult.error("修改操作必须传入部门名称");
        }
        List<SysDept> depts = getDeptsByConditions(deptBo);
        if (depts.isEmpty()) {
            return AjaxResult.error("部门不存在：" + deptBo.getDeptName());
        }
        if (depts.size() > 1) {
            return AjaxResult.error(buildMultipleDeptMessage("查询到多个部门，请补充更多信息（如负责人、联系电话等）后重试：", depts));
        }

        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);
        sysDept.setDeptId(depts.getFirst().getDeptId());

        if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
            if (parentDepts.isEmpty()) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            if (parentDepts.size() > 1) {
                return AjaxResult.error(buildMultipleDeptMessage("查询到多个父部门，请补充更多信息后重试：", parentDepts));
            }
            sysDept.setParentId(parentDepts.getFirst().getDeptId());
        }

        R<Boolean> result = remoteDeptService.updateDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDeptDelete(SysDeptBo deptBo) {
        if (deptBo.getDeptName() == null || deptBo.getDeptName().isEmpty()) {
            return AjaxResult.error("删除操作必须传入部门名称");
        }

        List<SysDept> depts = getDeptsByConditions(deptBo);
        if (depts.isEmpty()) {
            return AjaxResult.error("部门不存在：" + deptBo.getDeptName());
        }
        if (depts.size() > 1) {
            return AjaxResult.error(buildMultipleDeptMessage("查询到多个部门，请补充更多信息（如负责人、联系电话等）后重试：", depts));
        }

        R<Boolean> result = remoteDeptService.deleteDept(depts.getFirst().getDeptId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }
}
