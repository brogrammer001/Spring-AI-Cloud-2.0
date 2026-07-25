package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysUserBo;
import com.mall.chatmcp.bo.UserRoleBo;
import com.mall.chatmcp.bo.UserPostBo;
import com.mall.chatmcp.bo.UserDeptBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteUserService;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.RemoteRoleService;
import com.mall.system.api.RemotePostService;
import com.mall.system.api.domain.SysUser;
import com.mall.system.api.domain.SysDept;
import com.mall.system.api.domain.SysRole;
import com.mall.system.api.domain.SysPost;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class UserToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private RemoteRoleService remoteRoleService;

    @Autowired
    private RemotePostService remotePostService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "用户数据的新增、修改、删除。参数包含 operationType(add/update/delete)和用户实体。 [JSON]")
    public AjaxResult userCrud(SysUserBo userBo) {
        String operationType = userBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleAdd(userBo);
                case "update" -> handleUpdate(userBo);
                case "delete" -> handleDelete(userBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
            };
        }, "用户操作");
    }

    private AjaxResult handleAdd(SysUserBo userBo) {
        AjaxResult validateResult = validate(userBo, "sysUserBo");
        if (validateResult != null) {
            return validateResult;
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);

        if (userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
            Long deptId = getDeptIdByName(userBo.getDeptName());
            if (deptId == null) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            sysUser.setDeptId(deptId);
        }

        if ("男".equals(userBo.getSex())) {
            sysUser.setSex("0");
        } else if ("女".equals(userBo.getSex())) {
            sysUser.setSex("1");
        } else {
            sysUser.setSex("2");
        }

        sysUser.setPassword("admin123");
        sysUser.setAvatar("");
        sysUser.setStatus("0");

        R<Boolean> result = remoteUserService.addUserApi(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
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

    private AjaxResult handleUpdate(SysUserBo userBo) {
        if (userBo.getUserId() == null) {
            return AjaxResult.error("修改操作必须传入用户ID");
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);

        if (userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
            Long deptId = getDeptIdByName(userBo.getDeptName());
            if (deptId == null) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            sysUser.setDeptId(deptId);
        }

        if ("男".equals(userBo.getSex())) {
            sysUser.setSex("0");
        } else if ("女".equals(userBo.getSex())) {
            sysUser.setSex("1");
        }

        R<Boolean> result = remoteUserService.updateUser(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDelete(SysUserBo userBo) {
        if (userBo.getUserId() == null) {
            return AjaxResult.error("删除操作必须传入用户ID");
        }

        R<Boolean> result = remoteUserService.deleteUser(userBo.getUserId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "为用户分配角色。 [JSON]")
    public AjaxResult userRoleAuth(UserRoleBo userRoleBo) {
        return executeWithErrorHandling(() -> {
            Long userId = getUserIdByUserName(userRoleBo.getUserName());
            if (userId == null) {
                return AjaxResult.error("用户不存在：" + userRoleBo.getUserName());
            }

            if (userRoleBo.getRoleNames() != null && userRoleBo.getRoleNames().length > 0) {
                Long[] roleIds = new Long[userRoleBo.getRoleNames().length];
                for (int i = 0; i < userRoleBo.getRoleNames().length; i++) {
                    Long roleId = getRoleIdByName(userRoleBo.getRoleNames()[i]);
                    if (roleId == null) {
                        return AjaxResult.error("角色不存在：" + userRoleBo.getRoleNames()[i]);
                    }
                    roleIds[i] = roleId;
                }
                R<Boolean> result = remoteUserService.authRole(userId, roleIds);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户角色分配成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入角色名称列表");
            }
        }, "用户角色分配");
    }

    @Tool(description = "为用户分配岗位。 [JSON]")
    public AjaxResult userPostAuth(UserPostBo userPostBo) {
        return executeWithErrorHandling(() -> {
            Long userId = getUserIdByUserName(userPostBo.getUserName());
            if (userId == null) {
                return AjaxResult.error("用户不存在：" + userPostBo.getUserName());
            }

            if (userPostBo.getPostNames() != null && userPostBo.getPostNames().length > 0) {
                StringBuilder errorMsg = new StringBuilder();
                for (String postName : userPostBo.getPostNames()) {
                    Long postId = getPostIdByName(postName);
                    if (postId == null) {
                        if (errorMsg.length() > 0) {
                            errorMsg.append("；");
                        }
                        errorMsg.append("岗位不存在：").append(postName);
                    }
                }
                if (errorMsg.length() > 0) {
                    return AjaxResult.error(errorMsg.toString());
                }
                return AjaxResult.success("用户岗位分配成功（注：岗位分配需要在用户编辑页面操作）");
            } else {
                return AjaxResult.error("请传入岗位名称列表");
            }
        }, "用户岗位分配");
    }

    @Tool(description = "修改用户所属部门。 [JSON]")
    public AjaxResult userDeptAuth(UserDeptBo userDeptBo) {
        return executeWithErrorHandling(() -> {
            Long userId = getUserIdByUserName(userDeptBo.getUserName());
            if (userId == null) {
                return AjaxResult.error("用户不存在：" + userDeptBo.getUserName());
            }

            if (userDeptBo.getDeptName() != null && !userDeptBo.getDeptName().isEmpty()) {
                Long deptId = getDeptIdByName(userDeptBo.getDeptName());
                if (deptId == null) {
                    return AjaxResult.error("部门不存在：" + userDeptBo.getDeptName());
                }
                SysUser sysUser = new SysUser();
                sysUser.setUserId(userId);
                sysUser.setDeptId(deptId);
                R<Boolean> result = remoteUserService.updateUser(sysUser);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户部门修改成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入部门名称");
            }
        }, "用户部门修改");
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

    private Long getPostIdByName(String postName) {
        SysPost query = new SysPost();
        query.setPostName(postName);
        R<List<SysPost>> result = remotePostService.getPostList(query);
        if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
            if (result.getData().size() > 1) {
                return null;
            }
            return result.getData().get(0).getPostId();
        }
        return null;
    }

}