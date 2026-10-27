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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

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

    @Tool(description = "角色数据的新增、修改、删除。参数包含 operationType(add/update/delete)和角色实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
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
        if (roleBo.getRoleName() == null || roleBo.getRoleName().isEmpty()) {
            return formMissing("修改操作必须传入角色名称，请补充", roleBo, "roleName");
        }
        List<SysRole> roles = getRolesByConditions(roleBo);
        if (roles.isEmpty()) {
            return AjaxResult.error("角色不存在：" + roleBo.getRoleName());
        }
        if (roles.size() > 1) {
            return formMissing("查询到多个角色，请补充以下信息以精确定位：" + formatRoleList(roles), roleBo);
        }

        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);
        sysRole.setRoleId(roles.getFirst().getRoleId());

        R<Boolean> result = remoteRoleService.updateRole(sysRole);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleRoleDelete(SysRoleBo roleBo) {
        if (roleBo.getRoleName() == null || roleBo.getRoleName().isEmpty()) {
            return formMissing("删除操作必须传入角色名称，请补充", roleBo, "roleName");
        }

        List<SysRole> roles = getRolesByConditions(roleBo);
        if (roles.isEmpty()) {
            return AjaxResult.error("角色不存在：" + roleBo.getRoleName());
        }
        if (roles.size() > 1) {
            return formMissing("查询到多个角色，请补充以下信息以精确定位：" + formatRoleList(roles), roleBo);
        }

        R<Boolean> result = remoteRoleService.deleteRole(roles.getFirst().getRoleId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "为角色分配数据权限范围（部门）。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
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
            List<SysRole> roles = getRolesByName(roleDeptBo.getRoleName());
            if (roles.isEmpty()) {
                return AjaxResult.error("角色不存在：" + roleDeptBo.getRoleName());
            }
            if (roles.size() > 1) {
                return formMissing("查询到多个角色，请补充以下信息以精确定位：" + formatRoleList(roles), roleDeptBo);
            }
            Long roleId = roles.getFirst().getRoleId();

            Long[] deptIds = new Long[roleDeptBo.getDeptNames().length];
            for (int i = 0; i < roleDeptBo.getDeptNames().length; i++) {
                List<SysDept> deptList = getDeptsByName(roleDeptBo.getDeptNames()[i]);
                if (deptList.isEmpty()) {
                    return AjaxResult.error("部门不存在：" + roleDeptBo.getDeptNames()[i]);
                }
                if (deptList.size() > 1) {
                    return formMissing("查询到多个部门，请补充以下信息以精确定位：" + roleDeptBo.getDeptNames()[i], roleDeptBo);
                }
                deptIds[i] = deptList.getFirst().getDeptId();
            }
            SysRole sysRole = new SysRole();
            sysRole.setRoleId(roleId);
            sysRole.setDeptIds(deptIds);
            R<Boolean> result = remoteRoleService.authDataScope(sysRole);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色数据权限分配成功") : AjaxResult.error(result.getMsg());
        }, "角色数据权限分配");
    }

    @Tool(description = "为角色分配菜单权限。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
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
            List<SysRole> roles = getRolesByName(roleMenuBo.getRoleName());
            if (roles.isEmpty()) {
                return AjaxResult.error("角色不存在：" + roleMenuBo.getRoleName());
            }
            if (roles.size() > 1) {
                return formMissing("查询到多个角色，请补充以下信息以精确定位：" + formatRoleList(roles), roleMenuBo);
            }
            Long roleId = roles.getFirst().getRoleId();

            Long[] menuIds = new Long[roleMenuBo.getMenuNames().length];
            for (int i = 0; i < roleMenuBo.getMenuNames().length; i++) {
                List<SysMenuVo> menuList = getMenusByName(roleMenuBo.getMenuNames()[i]);
                if (menuList.isEmpty()) {
                    return AjaxResult.error("菜单不存在：" + roleMenuBo.getMenuNames()[i]);
                }
                if (menuList.size() > 1) {
                    return formMissing("查询到多个菜单，请补充以下信息以精确定位：" + roleMenuBo.getMenuNames()[i], roleMenuBo);
                }
                menuIds[i] = menuList.getFirst().getMenuId();
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

    private String formatRoleList(List<SysRole> roles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.size(); i++) {
            SysRole role = roles.get(i);
            sb.append(String.format("\n%d. 角色名称：%s，权限字符：%s，状态：%s",
                    i + 1, role.getRoleName(), role.getRoleKey(),
                    "0".equals(role.getStatus()) ? "正常" : "停用"));
        }
        return sb.toString();
    }
}
