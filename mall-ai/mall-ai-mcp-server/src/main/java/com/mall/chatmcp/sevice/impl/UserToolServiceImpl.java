package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysUserBo;
import com.mall.chatmcp.bo.UserDeptBo;
import com.mall.chatmcp.bo.UserPostBo;
import com.mall.chatmcp.bo.UserRoleBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDeptService;
import com.mall.system.api.RemotePostService;
import com.mall.system.api.RemoteRoleService;
import com.mall.system.api.RemoteUserService;
import com.mall.system.api.domain.SysDept;
import com.mall.system.api.domain.SysPost;
import com.mall.system.api.domain.SysRole;
import com.mall.system.api.domain.SysUser;
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

    @Tool(description = "用户数据的新增、修改、删除。参数包含 operationType(add/update/delete)和用户实体。")
    public AjaxResult userCrud(SysUserBo userBo) {
        String operationType = userBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleAdd(userBo);
            case "update" -> handleUpdate(userBo);
            case "delete" -> handleDelete(userBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
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
            List<SysDept> depts = getDeptsByName(userBo.getDeptName());
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            if (depts.size() > 1) {
                return AjaxResult.error("查询到多个部门，请补充更多信息：" + formatDeptList(depts));
            }
            sysUser.setDeptId(depts.getFirst().getDeptId());
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

    private AjaxResult handleUpdate(SysUserBo userBo) {
        if (userBo.getUserName() == null || userBo.getUserName().isEmpty()) {
            return AjaxResult.error("修改操作必须传入用户名");
        }
        List<SysUser> users = getUsersByConditions(userBo);
        if (users.isEmpty()) {
            return AjaxResult.error("用户不存在：" + userBo.getUserName());
        }
        if (users.size() > 1) {
            return AjaxResult.error("查询到多个用户，请补充更多信息（如昵称、手机号、邮箱等）后重试：" + formatUserList(users));
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);
        sysUser.setUserId(users.getFirst().getUserId());

        if (userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
            List<SysDept> depts = getDeptsByName(userBo.getDeptName());
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            if (depts.size() > 1) {
                return AjaxResult.error("查询到多个部门，请补充更多信息：" + formatDeptList(depts));
            }
            sysUser.setDeptId(depts.getFirst().getDeptId());
        }

        if ("男".equals(userBo.getSex())) {
            sysUser.setSex("0");
        } else if ("女".equals(userBo.getSex())) {
            sysUser.setSex("1");
        }

        if (userBo.getStatus() != null && !userBo.getStatus().isEmpty()) {
            if ("有效".equals(userBo.getStatus()) || "启用".equals(userBo.getStatus())) {
                sysUser.setStatus("0");
            } else if ("无效".equals(userBo.getStatus()) || "停用".equals(userBo.getStatus())) {
                sysUser.setStatus("1");
            } else {
                return AjaxResult.error("状态值不正确，请传入'有效'或'无效'");
            }
        }

        R<Boolean> result = remoteUserService.updateUser(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDelete(SysUserBo userBo) {
        if (userBo.getUserName() == null || userBo.getUserName().isEmpty()) {
            return AjaxResult.error("删除操作必须传入用户名");
        }
        List<SysUser> users = getUsersByConditions(userBo);
        if (users.isEmpty()) {
            return AjaxResult.error("用户不存在：" + userBo.getUserName());
        }
        if (users.size() > 1) {
            return AjaxResult.error("查询到多个用户，请补充更多信息（如昵称、手机号、邮箱等）后重试：" + formatUserList(users));
        }

        R<Boolean> result = remoteUserService.deleteUser(users.getFirst().getUserId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "为用户分配角色。 ")
    public AjaxResult userRoleAuth(UserRoleBo userRoleBo) {
        return executeWithErrorHandling(() -> {
            List<SysUser> users = getUsersByName(userRoleBo.getUserName());
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userRoleBo.getUserName());
            }
            if (users.size() > 1) {
                return AjaxResult.error("查询到多个用户，请补充更多信息：" + formatUserList(users));
            }
            Long userId = users.getFirst().getUserId();

            if (userRoleBo.getRoleNames() != null && userRoleBo.getRoleNames().length > 0) {
                Long[] roleIds = new Long[userRoleBo.getRoleNames().length];
                for (int i = 0; i < userRoleBo.getRoleNames().length; i++) {
                    List<SysRole> roles = getRolesByName(userRoleBo.getRoleNames()[i]);
                    if (roles.isEmpty()) {
                        return AjaxResult.error("角色不存在：" + userRoleBo.getRoleNames()[i]);
                    }
                    if (roles.size() > 1) {
                        return AjaxResult.error("查询到多个角色，请补充更多信息：" + formatRoleList(roles));
                    }
                    roleIds[i] = roles.getFirst().getRoleId();
                }
                R<Boolean> result = remoteUserService.authRole(userId, roleIds);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户角色分配成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入角色名称列表");
            }
        }, "用户角色分配");
    }

    @Tool(description = "为用户分配岗位。 ")
    public AjaxResult userPostAuth(UserPostBo userPostBo) {
        return executeWithErrorHandling(() -> {
            List<SysUser> users = getUsersByName(userPostBo.getUserName());
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userPostBo.getUserName());
            }
            if (users.size() > 1) {
                return AjaxResult.error("查询到多个用户，请补充更多信息：" + formatUserList(users));
            }

            if (userPostBo.getPostNames() != null && userPostBo.getPostNames().length > 0) {
                StringBuilder errorMsg = new StringBuilder();
                for (String postName : userPostBo.getPostNames()) {
                    List<SysPost> posts = getPostsByName(postName);
                    if (posts.isEmpty()) {
                        if (!errorMsg.isEmpty()) {
                            errorMsg.append("；");
                        }
                        errorMsg.append("岗位不存在：").append(postName);
                    } else if (posts.size() > 1) {
                        if (!errorMsg.isEmpty()) {
                            errorMsg.append("；");
                        }
                        errorMsg.append("查询到多个岗位，请补充更多信息：").append(formatPostList(posts));
                    }
                }
                if (!errorMsg.isEmpty()) {
                    return AjaxResult.error(errorMsg.toString());
                }
                return AjaxResult.success("用户岗位分配成功（注：岗位分配需要在用户编辑页面操作）");
            } else {
                return AjaxResult.error("请传入岗位名称列表");
            }
        }, "用户岗位分配");
    }

    @Tool(description = "修改用户所属部门。 ")
    public AjaxResult userDeptAuth(UserDeptBo userDeptBo) {
        return executeWithErrorHandling(() -> {
            List<SysUser> users = getUsersByName(userDeptBo.getUserName());
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userDeptBo.getUserName());
            }
            if (users.size() > 1) {
                return AjaxResult.error("查询到多个用户，请补充更多信息：" + formatUserList(users));
            }
            Long userId = users.getFirst().getUserId();

            if (userDeptBo.getDeptName() != null && !userDeptBo.getDeptName().isEmpty()) {
                List<SysDept> depts = getDeptsByName(userDeptBo.getDeptName());
                if (depts.isEmpty()) {
                    return AjaxResult.error("部门不存在：" + userDeptBo.getDeptName());
                }
                if (depts.size() > 1) {
                    return AjaxResult.error("查询到多个部门，请补充更多信息：" + formatDeptList(depts));
                }
                SysUser sysUser = new SysUser();
                sysUser.setUserId(userId);
                sysUser.setDeptId(depts.getFirst().getDeptId());
                R<Boolean> result = remoteUserService.updateUser(sysUser);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户部门修改成功") : AjaxResult.error(result.getMsg());
            } else {
                return AjaxResult.error("请传入部门名称");
            }
        }, "用户部门修改");
    }

    private List<SysUser> getUsersByName(String userName) {
        SysUser query = new SysUser();
        query.setUserName(userName);
        R<List<SysUser>> result = remoteUserService.getUserList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysUser> getUsersByConditions(SysUserBo userBo) {
        SysUser query = new SysUser();
        if (userBo.getUserName() != null && !userBo.getUserName().isEmpty()) {
            query.setUserName(userBo.getUserName());
        }
        if (userBo.getNickName() != null && !userBo.getNickName().isEmpty()) {
            query.setNickName(userBo.getNickName());
        }
        if (userBo.getPhonenumber() != null && !userBo.getPhonenumber().isEmpty()) {
            query.setPhonenumber(userBo.getPhonenumber());
        }
        if (userBo.getEmail() != null && !userBo.getEmail().isEmpty()) {
            query.setEmail(userBo.getEmail());
        }
        R<List<SysUser>> result = remoteUserService.getUserList(query);
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

    private List<SysRole> getRolesByName(String roleName) {
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        R<List<SysRole>> result = remoteRoleService.getRoleList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysPost> getPostsByName(String postName) {
        SysPost query = new SysPost();
        query.setPostName(postName);
        R<List<SysPost>> result = remotePostService.getPostList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private String formatUserList(List<SysUser> users) {
        StringBuilder sb = new StringBuilder();
        for (SysUser u : users) {
            sb.append("\n- 用户名：").append(u.getUserName())
                    .append("，昵称：").append(u.getNickName())
                    .append("，手机：").append(u.getPhonenumber() != null ? u.getPhonenumber() : "无")
                    .append("，邮箱：").append(u.getEmail() != null ? u.getEmail() : "无");
        }
        return sb.toString();
    }

    private String formatDeptList(List<SysDept> depts) {
        StringBuilder sb = new StringBuilder();
        for (SysDept d : depts) {
            sb.append("\n- 部门名称：").append(d.getDeptName())
                    .append("，负责人：").append(d.getLeader() != null ? d.getLeader() : "无")
                    .append("，联系电话：").append(d.getPhone() != null ? d.getPhone() : "无");
        }
        return sb.toString();
    }

    private String formatRoleList(List<SysRole> roles) {
        StringBuilder sb = new StringBuilder();
        for (SysRole r : roles) {
            sb.append("\n- 角色名称：").append(r.getRoleName())
                    .append("，权限字符：").append(r.getRoleKey())
                    .append("，状态：").append("0".equals(r.getStatus()) ? "正常" : "停用");
        }
        return sb.toString();
    }

    private String formatPostList(List<SysPost> posts) {
        StringBuilder sb = new StringBuilder();
        for (SysPost p : posts) {
            sb.append("\n- 岗位名称：").append(p.getPostName())
                    .append("，岗位编码：").append(p.getPostCode())
                    .append("，状态：").append("0".equals(p.getStatus()) ? "正常" : "停用");
        }
        return sb.toString();
    }

}
