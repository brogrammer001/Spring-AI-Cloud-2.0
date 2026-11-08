package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysDeptBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.domain.SysDept;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeptToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "部门数据的新增、修改、删除。参数包含 operationType(add/update/delete)和部门实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult deptCrud(SysDeptBo deptBo) {
        AjaxResult gate = crudGate(deptBo, "deptCrud", "部门", OPS_CRUD, "deptName");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (deptBo.getOperationType().toLowerCase()) {
            case "add" -> handleDeptAdd(deptBo);
            case "update" -> handleDeptUpdate(deptBo);
            case "delete" -> handleDeptDelete(deptBo);
            default -> AjaxResult.error("不支持的操作类型：" + deptBo.getOperationType() + "，请使用：add、update、delete");
        }, "部门操作");
    }

    private AjaxResult handleDeptAdd(SysDeptBo deptBo) {
        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);

        if (deptBo.getParentId() != null) {
            sysDept.setParentId(deptBo.getParentId());
        } else if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
            if (parentDepts.isEmpty()) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            if (parentDepts.size() > 1) {
                return formSelect("查询到 " + parentDepts.size() + " 个父部门，请选择作为上级的部门：",
                    deptBo, parentDepts, SysDept::getDeptId, this::deptLabel, "parentId", "父部门");
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

    private String deptLabel(SysDept d) {
        return "部门名称：" + d.getDeptName()
            + "，负责人：" + (d.getLeader() != null ? d.getLeader() : "无")
            + "，联系电话：" + (d.getPhone() != null ? d.getPhone() : "无");
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
        Long deptId = deptBo.getDeptId();
        if (deptId == null) {
            if (deptBo.getDeptName() == null || deptBo.getDeptName().isEmpty()) {
                return formMissing("修改操作必须传入部门名称，请补充", deptBo, "deptName");
            }
            List<SysDept> depts = getDeptsByConditions(deptBo);
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + deptBo.getDeptName());
            }
            if (depts.size() > 1) {
                return formSelect("查询到 " + depts.size() + " 个部门，请选择要修改的部门：",
                    deptBo, depts, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
            }
            deptId = depts.getFirst().getDeptId();
        }

        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);
        sysDept.setDeptId(deptId);

        if (deptBo.getParentId() != null) {
            sysDept.setParentId(deptBo.getParentId());
        } else if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
            List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
            if (parentDepts.isEmpty()) {
                return AjaxResult.error("父部门不存在：" + deptBo.getParentName());
            }
            if (parentDepts.size() > 1) {
                return formSelect("查询到 " + parentDepts.size() + " 个父部门，请选择作为上级的部门：",
                    deptBo, parentDepts, SysDept::getDeptId, this::deptLabel, "parentId", "父部门");
            }
            sysDept.setParentId(parentDepts.getFirst().getDeptId());
        }

        R<Boolean> result = remoteDeptService.updateDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDeptDelete(SysDeptBo deptBo) {
        Long deptId = deptBo.getDeptId();
        if (deptId == null) {
            if (deptBo.getDeptName() == null || deptBo.getDeptName().isEmpty()) {
                return formMissing("删除操作必须传入部门名称，请补充", deptBo, "deptName");
            }
            List<SysDept> depts = getDeptsByConditions(deptBo);
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + deptBo.getDeptName());
            }
            if (depts.size() > 1) {
                return formSelect("查询到 " + depts.size() + " 个部门，请选择要删除的部门：",
                    deptBo, depts, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
            }
            deptId = depts.getFirst().getDeptId();
        }

        R<Boolean> result = remoteDeptService.deleteDept(deptId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "批量新增部门。参数：部门列表(deptList)，每个元素需包含部门名称，可填写父部门名称、负责人、联系电话、排序等。未填父部门时挂到根部门。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddDepts(
            @ToolParam(description = "部门信息列表") List<SysDeptBo> deptList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(deptList, "部门");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysDeptBo deptBo : deptList) {
                AjaxResult validateResult = validate(deptBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("deptList", deptList.stream().map(this::toMap).toList());
                return confirmResult("batchAddDepts", "add", "确认批量新增部门",
                    "即将批量新增 " + deptList.size() + " 个部门，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下部门新增失败：");
            for (SysDeptBo deptBo : deptList) {
                SysDept sysDept = new SysDept();
                BeanUtils.copyProperties(deptBo, sysDept);

                Long parentId = deptBo.getParentId();
                if (parentId == null && deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
                    List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
                    if (parentDepts.isEmpty()) {
                        batch.addFail(deptBo.getDeptName(), "父部门不存在：" + deptBo.getParentName());
                        continue;
                    }
                    if (parentDepts.size() > 1) {
                        batch.addFail(deptBo.getDeptName(), "父部门名称匹配到多条记录，请携带父部门ID");
                        continue;
                    }
                    parentId = parentDepts.getFirst().getDeptId();
                }
                sysDept.setParentId(parentId == null ? 0L : parentId);
                sysDept.setStatus("0");

                R<Boolean> result = remoteDeptService.addDept(sysDept);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(deptBo.getDeptName());
                } else {
                    batch.addFail(deptBo.getDeptName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增部门");
    }

    @Tool(description = "批量删除部门。参数：部门名称列表(deptNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteDepts(
            @ToolParam(description = "部门名称列表，例如 [\"财务部\",\"研发部\"]") List<String> deptNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(deptNames, "部门");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("deptNames", deptNames);
                return confirmResult("batchDeleteDepts", "delete", "确认批量删除部门",
                    "即将批量删除 " + deptNames.size() + " 个部门：" + joinNames(deptNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下部门删除失败：");
            for (String deptName : deptNames) {
                List<SysDept> depts = getDeptsByName(deptName);
                if (depts.isEmpty()) {
                    batch.addFail(deptName, "部门不存在");
                    continue;
                }
                if (depts.size() > 1) {
                    batch.addFail(deptName, "匹配到多条记录，请使用更精确的部门名称或先查询部门ID");
                    continue;
                }
                R<Boolean> result = remoteDeptService.deleteDept(depts.getFirst().getDeptId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(deptName);
                } else {
                    batch.addFail(deptName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除部门");
    }

    @Tool(description = "批量修改部门。参数：部门列表(deptList)，每个元素需包含部门名称(deptName)或部门ID(deptId)用于定位，另可填写父部门名称、负责人、联系电话、排序、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateDepts(
            @ToolParam(description = "部门信息列表") List<SysDeptBo> deptList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(deptList, "部门");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("deptList", deptList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateDepts", "update", "确认批量修改部门",
                    "即将批量修改 " + deptList.size() + " 个部门，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下部门修改失败：");
            for (SysDeptBo deptBo : deptList) {
                String label = deptBo.getDeptName() != null && !deptBo.getDeptName().isEmpty()
                    ? deptBo.getDeptName() : "ID=" + deptBo.getDeptId();
                Long deptId = deptBo.getDeptId();
                if (deptId == null) {
                    if (deptBo.getDeptName() == null || deptBo.getDeptName().isEmpty()) {
                        batch.addFail(label, "缺少部门名称或部门ID");
                        continue;
                    }
                    List<SysDept> depts = getDeptsByName(deptBo.getDeptName());
                    if (depts.isEmpty()) {
                        batch.addFail(label, "部门不存在");
                        continue;
                    }
                    if (depts.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带部门ID");
                        continue;
                    }
                    deptId = depts.getFirst().getDeptId();
                }

                SysDept sysDept = new SysDept();
                BeanUtils.copyProperties(deptBo, sysDept);
                sysDept.setDeptId(deptId);

                Long parentId = deptBo.getParentId();
                if (parentId == null && deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
                    List<SysDept> parentDepts = getDeptsByName(deptBo.getParentName());
                    if (parentDepts.isEmpty()) {
                        batch.addFail(label, "父部门不存在：" + deptBo.getParentName());
                        continue;
                    }
                    if (parentDepts.size() > 1) {
                        batch.addFail(label, "父部门名称匹配到多条记录，请携带父部门ID");
                        continue;
                    }
                    parentId = parentDepts.getFirst().getDeptId();
                }
                if (parentId != null) {
                    sysDept.setParentId(parentId);
                }

                R<Boolean> result = remoteDeptService.updateDept(sysDept);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改部门");
    }
}
