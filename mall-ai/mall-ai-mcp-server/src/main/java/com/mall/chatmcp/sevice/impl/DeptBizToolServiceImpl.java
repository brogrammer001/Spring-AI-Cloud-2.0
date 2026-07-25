package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysDeptBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.RemoteRoleService;
import com.mall.system.api.RemoteUserService;
import com.mall.system.api.domain.SysDept;
import com.mall.system.api.domain.SysRole;
import com.mall.system.api.domain.SysUser;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class DeptBizToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteRoleService remoteRoleService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "创建部门并设置负责人，自动为负责人分配部门管理员角色。参数：部门信息(dept)、负责人用户名(adminUserName)。 [JSON]")
    public AjaxResult createDeptWithAdmin(
            @ToolParam(description = "部门信息") SysDeptBo dept,
            @ToolParam(description = "负责人用户名") String adminUserName) {

        return executeWithErrorHandling(() -> {
            AjaxResult validateResult = validate(dept, "sysDeptBo");
            if (validateResult != null) {
                return validateResult;
            }

            if (adminUserName == null || adminUserName.isEmpty()) {
                return AjaxResult.error("负责人用户名不能为空");
            }

            Long userId = getUserIdByUserName(adminUserName);
            if (userId == null) {
                return AjaxResult.error("负责人用户不存在：" + adminUserName);
            }

            SysDept sysDept = new SysDept();
            BeanUtils.copyProperties(dept, sysDept);

            if (dept.getParentName() != null && !dept.getParentName().isEmpty()) {
                Long parentId = getDeptIdByName(dept.getParentName());
                if (parentId == null) {
                    return AjaxResult.error("父部门不存在：" + dept.getParentName());
                }
                sysDept.setParentId(parentId);
            } else {
                sysDept.setParentId(0L);
            }

            sysDept.setStatus("0");
            sysDept.setLeader(adminUserName);

            R<Boolean> deptResult = remoteDeptService.addDept(sysDept);
            if (!(deptResult.getCode() == 200 && deptResult.getData())) {
                return AjaxResult.error("部门创建失败：" + deptResult.getMsg());
            }

            Long deptId = getDeptIdByName(dept.getDeptName());
            if (deptId == null) {
                return AjaxResult.error("部门创建成功，但无法获取部门ID");
            }

            R<Boolean> updateUserResult = updateUserDept(userId, deptId);
            if (!(updateUserResult.getCode() == 200 && updateUserResult.getData())) {
                return AjaxResult.error("部门创建成功，但负责人部门分配失败：" + updateUserResult.getMsg());
            }

            Long adminRoleId = getOrCreateDeptAdminRole(dept.getDeptName());
            if (adminRoleId != null) {
                R<Boolean> authResult = remoteUserService.authRole(userId, new Long[]{adminRoleId});
                if (!(authResult.getCode() == 200 && authResult.getData())) {
                    logger.warn("部门管理员角色分配失败：" + authResult.getMsg());
                }
            }

            return AjaxResult.success("部门创建成功，负责人已设置并分配部门管理员角色");
        }, "创建部门并设置负责人");
    }

    @Tool(description = "批量创建部门结构。参数：部门信息列表(deptList)。按顺序创建，支持父子关系。 [JSON]")
    public AjaxResult batchCreateDepts(
            @ToolParam(description = "部门信息列表") List<SysDeptBo> deptList) {

        return executeWithErrorHandling(() -> {
            if (deptList == null || deptList.isEmpty()) {
                return AjaxResult.error("部门列表不能为空");
            }

            StringBuilder successMsg = new StringBuilder("成功创建以下部门：");
            StringBuilder failMsg = new StringBuilder();

            for (SysDeptBo deptBo : deptList) {
                AjaxResult validateResult = validate(deptBo, "sysDeptBo");
                if (validateResult != null) {
                    if (failMsg.length() > 0) {
                        failMsg.append("；");
                    }
                    failMsg.append(deptBo.getDeptName()).append("：").append(validateResult.get("msg"));
                    continue;
                }

                SysDept sysDept = new SysDept();
                BeanUtils.copyProperties(deptBo, sysDept);

                if (deptBo.getParentName() != null && !deptBo.getParentName().isEmpty()) {
                    Long parentId = getDeptIdByName(deptBo.getParentName());
                    if (parentId == null) {
                        if (failMsg.length() > 0) {
                            failMsg.append("；");
                        }
                        failMsg.append(deptBo.getDeptName()).append("：父部门不存在").append(deptBo.getParentName());
                        continue;
                    }
                    sysDept.setParentId(parentId);
                } else {
                    sysDept.setParentId(0L);
                }

                sysDept.setStatus("0");

                R<Boolean> result = remoteDeptService.addDept(sysDept);
                if (result.getCode() == 200 && result.getData()) {
                    if (successMsg.length() > 10) {
                        successMsg.append("、");
                    }
                    successMsg.append(deptBo.getDeptName());
                } else {
                    if (failMsg.length() > 0) {
                        failMsg.append("；");
                    }
                    failMsg.append(deptBo.getDeptName()).append("：").append(result.getMsg());
                }
            }

            if (failMsg.length() > 0) {
                return AjaxResult.error(successMsg.toString() + "。以下部门创建失败：" + failMsg.toString());
            }
            return AjaxResult.success(successMsg.toString());
        }, "批量创建部门");
    }

    private Long getUserIdByUserName(String userName) {
        SysUser query = new SysUser();
        query.setUserName(userName);
        R<List<SysUser>> result = remoteUserService.getUserList(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            if (result.getData().size() > 1) {
                return null;
            }
            return result.getData().get(0).getUserId();
        }
        return null;
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

    private R<Boolean> updateUserDept(Long userId, Long deptId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setDeptId(deptId);
        return remoteUserService.updateUser(sysUser);
    }

    private Long getOrCreateDeptAdminRole(String deptName) {
        String roleName = deptName + "管理员";
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        R<List<SysRole>> result = remoteRoleService.getRoleList(query);

        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            return result.getData().get(0).getRoleId();
        }

        SysRole newRole = new SysRole();
        newRole.setRoleName(roleName);
        newRole.setRoleKey(deptName.toLowerCase() + "_admin");
        newRole.setRoleSort(1);
        newRole.setStatus("0");
        newRole.setDataScope("1");

        R<Boolean> addResult = remoteRoleService.addRole(newRole);
        if (addResult.getCode() == 200 && addResult.getData()) {
            result = remoteRoleService.getRoleList(query);
            if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                return result.getData().get(0).getRoleId();
            }
        }

        return null;
    }

}