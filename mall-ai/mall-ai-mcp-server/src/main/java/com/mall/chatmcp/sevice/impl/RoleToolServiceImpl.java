package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.RoleDeptBo;
import com.mall.chatmcp.bo.RoleMenuBo;
import com.mall.chatmcp.bo.SysRoleBo;
import com.mall.chatmcp.sevice.BaseToolService;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class RoleToolServiceImpl implements BaseToolService {

    @Autowired
    private RemoteRoleService remoteRoleService;

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private RemoteMenuService remoteMenuService;

    @Autowired
    private Validator validator;

    @Tool(description = "角色数据的新增、修改、删除。参数包含 operationType(add/update/delete)和角色实体。 [JSON]")
    public AjaxResult roleCrud(SysRoleBo roleBo) {
        String operationType = roleBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleRoleAdd(roleBo);
                case "update" -> handleRoleUpdate(roleBo);
                case "delete" -> handleRoleDelete(roleBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handleRoleAdd(SysRoleBo roleBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(roleBo, "sysRoleBo");
        validator.validate(roleBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);

        if (sysRole.getStatus() == null || sysRole.getStatus().isEmpty()) {
            sysRole.setStatus("0");
        }

        R<Boolean> result = remoteRoleService.addRole(sysRole);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleRoleUpdate(SysRoleBo roleBo) {
        if (roleBo.getRoleId() == null) {
            return AjaxResult.error("修改操作必须传入角色ID");
        }
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);
        R<Boolean> result = remoteRoleService.updateRole(sysRole);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleRoleDelete(SysRoleBo roleBo) {
        if (roleBo.getRoleId() == null) {
            return AjaxResult.error("删除操作必须传入角色ID");
        }
        R<Boolean> result = remoteRoleService.deleteRole(roleBo.getRoleId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "为角色分配数据权限范围（部门）。 [JSON]")
    public AjaxResult roleDeptAuth(RoleDeptBo roleDeptBo) {
        Long roleId = getRoleIdByName(roleDeptBo.getRoleName());
        if (roleId == null) {
            return AjaxResult.error("角色不存在：" + roleDeptBo.getRoleName());
        }

        try {
            if (roleDeptBo.getDeptNames() != null && roleDeptBo.getDeptNames().length > 0) {
                Long[] deptIds = new Long[roleDeptBo.getDeptNames().length];
                for (int i = 0; i < roleDeptBo.getDeptNames().length; i++) {
                    Long deptId = getDeptIdByName(roleDeptBo.getDeptNames()[i]);
                    if (deptId == null) {
                        return AjaxResult.error("部门不存在：" + roleDeptBo.getDeptNames()[i]);
                    }
                    deptIds[i] = deptId;
                }
                SysRole sysRole = new SysRole();
                sysRole.setRoleId(roleId);
                sysRole.setDeptIds(deptIds);
                R<Boolean> result = remoteRoleService.authDataScope(sysRole);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色数据权限分配成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入部门名称列表");
            }
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，角色数据权限分配失败，请稍后再试。");
        }
    }

    @Tool(description = "为角色分配菜单权限。 [JSON]")
    public AjaxResult roleMenuAuth(RoleMenuBo roleMenuBo) {
        Long roleId = getRoleIdByName(roleMenuBo.getRoleName());
        if (roleId == null) {
            return AjaxResult.error("角色不存在：" + roleMenuBo.getRoleName());
        }

        try {
            if (roleMenuBo.getMenuNames() != null && roleMenuBo.getMenuNames().length > 0) {
                Long[] menuIds = new Long[roleMenuBo.getMenuNames().length];
                for (int i = 0; i < roleMenuBo.getMenuNames().length; i++) {
                    Long menuId = getMenuIdByName(roleMenuBo.getMenuNames()[i]);
                    if (menuId == null) {
                        return AjaxResult.error("菜单不存在：" + roleMenuBo.getMenuNames()[i]);
                    }
                    menuIds[i] = menuId;
                }
                SysRole sysRole = new SysRole();
                sysRole.setRoleId(roleId);
                sysRole.setMenuIds(menuIds);
                R<Boolean> result = remoteRoleService.authMenu(sysRole);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色菜单权限分配成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入菜单名称列表");
            }
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，角色菜单权限操作失败，请稍后再试。");
        }
    }

    private Long getRoleIdByName(String roleName) {
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        R<List<SysRole>> result = remoteRoleService.getRoleList(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            if (result.getData().size() > 1) {
                return null;
            }
            return result.getData().get(0).getRoleId();
        }
        return null;
    }

    private Long getDeptIdByName(String deptName) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        R<List<SysDept>> result = remoteDeptService.getDeptList(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            if (result.getData().size() > 1) {
                return null;
            }
            return result.getData().get(0).getDeptId();
        }
        return null;
    }

    private Long getMenuIdByName(String menuName) {
        SysMenuVo query = new SysMenuVo();
        query.setMenuName(menuName);
        R<List<SysMenuVo>> result = remoteMenuService.getMenuByParam(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            if (result.getData().size() > 1) {
                return null;
            }
            return result.getData().get(0).getMenuId();
        }
        return null;
    }

}