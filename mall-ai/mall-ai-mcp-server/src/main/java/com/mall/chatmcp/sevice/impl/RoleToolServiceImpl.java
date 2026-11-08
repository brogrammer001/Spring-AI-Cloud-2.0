package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.RoleDeptBo;
import com.mall.chatmcp.bo.RoleMenuBo;
import com.mall.chatmcp.bo.SysRoleBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.RemoteMenuService;
import com.mall.system.api.RemoteRoleService;
import com.mall.system.api.domain.SysDept;
import com.mall.system.api.domain.SysMenuVo;
import com.mall.system.api.domain.SysRole;
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
public class RoleToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteRoleService remoteRoleService;

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private RemoteMenuService remoteMenuService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "角色数据的新增、修改、删除。参数包含 operationType(add/update/delete)和角色实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult roleCrud(SysRoleBo roleBo) {
        AjaxResult gate = crudGate(roleBo, "roleCrud", "角色", OPS_CRUD, "roleName");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (roleBo.getOperationType().toLowerCase()) {
            case "add" -> handleRoleAdd(roleBo);
            case "update" -> handleRoleUpdate(roleBo);
            case "delete" -> handleRoleDelete(roleBo);
            default -> AjaxResult.error("不支持的操作类型：" + roleBo.getOperationType() + "，请使用：add、update、delete");
        }, "角色操作");
    }

    private AjaxResult handleRoleAdd(SysRoleBo roleBo) {
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);

        if (sysRole.getStatus() == null || sysRole.getStatus().isEmpty()) {
            sysRole.setStatus("0");
        }

        R<Boolean> result = remoteRoleService.addRole(sysRole);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleRoleUpdate(SysRoleBo roleBo) {
        Long roleId = roleBo.getRoleId();
        if (roleId == null) {
            if (roleBo.getRoleName() == null || roleBo.getRoleName().isEmpty()) {
                return formMissing("修改操作必须传入角色名称，请补充", roleBo, "roleName");
            }
            List<SysRole> roles = getRolesByConditions(roleBo);
            if (roles.isEmpty()) {
                return AjaxResult.error("角色不存在：" + roleBo.getRoleName());
            }
            if (roles.size() > 1) {
                return formSelect("查询到 " + roles.size() + " 个角色，请选择要修改的角色：",
                    roleBo, roles, SysRole::getRoleId, this::roleLabel, "roleId", "角色");
            }
            roleId = roles.getFirst().getRoleId();
        }

        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);
        sysRole.setRoleId(roleId);

        R<Boolean> result = remoteRoleService.updateRole(sysRole);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleRoleDelete(SysRoleBo roleBo) {
        Long roleId = roleBo.getRoleId();
        if (roleId == null) {
            if (roleBo.getRoleName() == null || roleBo.getRoleName().isEmpty()) {
                return formMissing("删除操作必须传入角色名称，请补充", roleBo, "roleName");
            }
            List<SysRole> roles = getRolesByConditions(roleBo);
            if (roles.isEmpty()) {
                return AjaxResult.error("角色不存在：" + roleBo.getRoleName());
            }
            if (roles.size() > 1) {
                return formSelect("查询到 " + roles.size() + " 个角色，请选择要删除的角色：",
                    roleBo, roles, SysRole::getRoleId, this::roleLabel, "roleId", "角色");
            }
            roleId = roles.getFirst().getRoleId();
        }

        R<Boolean> result = remoteRoleService.deleteRole(roleId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "批量新增角色。参数：角色列表(roleList)，每个元素需包含角色名称、权限字符等必填信息，可填写排序、数据范围、状态、备注。未填状态默认为正常。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddRoles(
            @ToolParam(description = "角色信息列表") List<SysRoleBo> roleList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(roleList, "角色");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysRoleBo roleBo : roleList) {
                AjaxResult validateResult = validate(roleBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("roleList", roleList.stream().map(this::toMap).toList());
                return confirmResult("batchAddRoles", "add", "确认批量新增角色",
                    "即将批量新增 " + roleList.size() + " 个角色，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下角色新增失败：");
            for (SysRoleBo roleBo : roleList) {
                SysRole sysRole = new SysRole();
                BeanUtils.copyProperties(roleBo, sysRole);
                if (sysRole.getStatus() == null || sysRole.getStatus().isEmpty()) {
                    sysRole.setStatus("0");
                }
                R<Boolean> result = remoteRoleService.addRole(sysRole);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(roleBo.getRoleName());
                } else {
                    batch.addFail(roleBo.getRoleName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增角色");
    }

    @Tool(description = "批量删除角色。参数：角色名称列表(roleNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteRoles(
            @ToolParam(description = "角色名称列表，例如 [\"管理员\",\"普通用户\"]") List<String> roleNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(roleNames, "角色");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("roleNames", roleNames);
                return confirmResult("batchDeleteRoles", "delete", "确认批量删除角色",
                    "即将批量删除 " + roleNames.size() + " 个角色：" + joinNames(roleNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下角色删除失败：");
            for (String roleName : roleNames) {
                List<SysRole> roles = getRolesByName(roleName);
                if (roles.isEmpty()) {
                    batch.addFail(roleName, "角色不存在");
                    continue;
                }
                if (roles.size() > 1) {
                    batch.addFail(roleName, "匹配到多条记录，请使用更精确的角色名称或先查询角色ID");
                    continue;
                }
                R<Boolean> result = remoteRoleService.deleteRole(roles.getFirst().getRoleId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(roleName);
                } else {
                    batch.addFail(roleName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除角色");
    }

    @Tool(description = "批量修改角色。参数：角色列表(roleList)，每个元素需包含角色名称(roleName)或角色ID(roleId)用于定位，另可填写权限字符、排序、数据范围、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateRoles(
            @ToolParam(description = "角色信息列表") List<SysRoleBo> roleList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(roleList, "角色");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("roleList", roleList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateRoles", "update", "确认批量修改角色",
                    "即将批量修改 " + roleList.size() + " 个角色，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下角色修改失败：");
            for (SysRoleBo roleBo : roleList) {
                String label = roleBo.getRoleName() != null && !roleBo.getRoleName().isEmpty()
                    ? roleBo.getRoleName() : "ID=" + roleBo.getRoleId();
                Long roleId = roleBo.getRoleId();
                if (roleId == null) {
                    if (roleBo.getRoleName() == null || roleBo.getRoleName().isEmpty()) {
                        batch.addFail(label, "缺少角色名称或角色ID");
                        continue;
                    }
                    List<SysRole> roles = getRolesByName(roleBo.getRoleName());
                    if (roles.isEmpty()) {
                        batch.addFail(label, "角色不存在");
                        continue;
                    }
                    if (roles.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带角色ID");
                        continue;
                    }
                    roleId = roles.getFirst().getRoleId();
                }

                SysRole sysRole = new SysRole();
                BeanUtils.copyProperties(roleBo, sysRole);
                sysRole.setRoleId(roleId);
                R<Boolean> result = remoteRoleService.updateRole(sysRole);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改角色");
    }

    @Tool(description = "为角色分配数据权限范围（部门）。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult roleDeptAuth(RoleDeptBo roleDeptBo) {
        AjaxResult validateResult = validate(roleDeptBo);
        if (validateResult != null) {
            return validateResult;
        }
        if (roleDeptBo.getDeptNames() == null || roleDeptBo.getDeptNames().length == 0) {
            return formMissing("请传入部门名称列表", roleDeptBo, "deptNames");
        }
        if (!isConfirmed(roleDeptBo)) {
            return confirmResult("roleDeptAuth", "auth", "确认角色数据权限分配",
                "即将为角色「" + roleDeptBo.getRoleName() + "」分配数据权限，请确认是否继续。", toMap(roleDeptBo));
        }
        return executeWithErrorHandling(() -> {
            Long roleId = roleDeptBo.getRoleId();
            if (roleId == null) {
                List<SysRole> roles = getRolesByName(roleDeptBo.getRoleName());
                if (roles.isEmpty()) {
                    return AjaxResult.error("角色不存在：" + roleDeptBo.getRoleName());
                }
                if (roles.size() > 1) {
                    return formSelect("查询到 " + roles.size() + " 个角色，请选择要分配数据权限的角色：",
                        roleDeptBo, roles, SysRole::getRoleId, this::roleLabel, "roleId", "角色");
                }
                roleId = roles.getFirst().getRoleId();
            }

            Long[] deptIds = new Long[roleDeptBo.getDeptNames().length];
            for (int i = 0; i < roleDeptBo.getDeptNames().length; i++) {
                String deptName = roleDeptBo.getDeptNames()[i];
                List<SysDept> deptList = getDeptsByName(deptName);
                if (deptList.isEmpty()) {
                    return AjaxResult.error("部门不存在：" + deptName);
                }
                if (deptList.size() > 1) {
                    Long selectedDeptId = matchSelectedId(deptList, roleDeptBo.getDeptId(), SysDept::getDeptId);
                    if (selectedDeptId == null) {
                        return formSelect("查询到 " + deptList.size() + " 个名称为「" + deptName + "」的部门，请选择：",
                            roleDeptBo, deptList, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
                    }
                    deptIds[i] = selectedDeptId;
                } else {
                    deptIds[i] = deptList.getFirst().getDeptId();
                }
            }
            SysRole sysRole = new SysRole();
            sysRole.setRoleId(roleId);
            sysRole.setDeptIds(deptIds);
            R<Boolean> result = remoteRoleService.authDataScope(sysRole);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色数据权限分配成功") : AjaxResult.error(result.getMsg());
        }, "角色数据权限分配");
    }

    @Tool(description = "为角色分配菜单权限。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult roleMenuAuth(RoleMenuBo roleMenuBo) {
        AjaxResult validateResult = validate(roleMenuBo);
        if (validateResult != null) {
            return validateResult;
        }
        if (roleMenuBo.getMenuNames() == null || roleMenuBo.getMenuNames().length == 0) {
            return formMissing("请传入菜单名称列表", roleMenuBo, "menuNames");
        }
        if (!isConfirmed(roleMenuBo)) {
            return confirmResult("roleMenuAuth", "auth", "确认角色菜单权限分配",
                "即将为角色「" + roleMenuBo.getRoleName() + "」分配菜单权限，请确认是否继续。", toMap(roleMenuBo));
        }
        return executeWithErrorHandling(() -> {
            Long roleId = roleMenuBo.getRoleId();
            if (roleId == null) {
                List<SysRole> roles = getRolesByName(roleMenuBo.getRoleName());
                if (roles.isEmpty()) {
                    return AjaxResult.error("角色不存在：" + roleMenuBo.getRoleName());
                }
                if (roles.size() > 1) {
                    return formSelect("查询到 " + roles.size() + " 个角色，请选择要分配菜单权限的角色：",
                        roleMenuBo, roles, SysRole::getRoleId, this::roleLabel, "roleId", "角色");
                }
                roleId = roles.getFirst().getRoleId();
            }

            Long[] menuIds = new Long[roleMenuBo.getMenuNames().length];
            for (int i = 0; i < roleMenuBo.getMenuNames().length; i++) {
                String menuName = roleMenuBo.getMenuNames()[i];
                List<SysMenuVo> menuList = getMenusByName(menuName);
                if (menuList.isEmpty()) {
                    return AjaxResult.error("菜单不存在：" + menuName);
                }
                if (menuList.size() > 1) {
                    Long selectedMenuId = matchSelectedId(menuList, roleMenuBo.getMenuId(), SysMenuVo::getMenuId);
                    if (selectedMenuId == null) {
                        return formSelect("查询到 " + menuList.size() + " 个名称为「" + menuName + "」的菜单，请选择：",
                            roleMenuBo, menuList, SysMenuVo::getMenuId, this::menuLabel, "menuId", "菜单");
                    }
                    menuIds[i] = selectedMenuId;
                } else {
                    menuIds[i] = menuList.getFirst().getMenuId();
                }
            }
            SysRole sysRole = new SysRole();
            sysRole.setRoleId(roleId);
            sysRole.setMenuIds(menuIds);
            R<Boolean> result = remoteRoleService.authMenu(sysRole);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色菜单权限分配成功") : AjaxResult.error(result.getMsg());
        }, "角色菜单权限分配");
    }

    private List<SysRole> getRolesByName(String roleName) {
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        R<List<SysRole>> result = remoteRoleService.getRoleList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysRole> getRolesByConditions(SysRoleBo roleBo) {
        SysRole query = new SysRole();
        if (roleBo.getRoleName() != null && !roleBo.getRoleName().isEmpty()) {
            query.setRoleName(roleBo.getRoleName());
        }
        if (roleBo.getRoleKey() != null && !roleBo.getRoleKey().isEmpty()) {
            query.setRoleKey(roleBo.getRoleKey());
        }
        R<List<SysRole>> result = remoteRoleService.getRoleList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
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

    private List<SysMenuVo> getMenusByName(String menuName) {
        SysMenuVo query = new SysMenuVo();
        query.setMenuName(menuName);
        R<List<SysMenuVo>> result = remoteMenuService.getMenuByParam(query);
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

    private String menuLabel(SysMenuVo m) {
        return "菜单名称：" + m.getMenuName()
            + "，路由地址：" + (m.getPath() != null ? m.getPath() : "无");
    }

    private String roleLabel(SysRole r) {
        return "角色名称：" + r.getRoleName()
            + "，权限字符：" + r.getRoleKey()
            + "，状态：" + ("0".equals(r.getStatus()) ? "正常" : "停用");
    }
}
