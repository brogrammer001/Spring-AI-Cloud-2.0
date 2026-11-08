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
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Tool(description = "用户数据的新增、修改、删除。参数包含 operationType(add/update/delete)和用户实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult userCrud(SysUserBo userBo) {
        AjaxResult gate = crudGate(userBo, "userCrud", "用户", OPS_CRUD, "userName");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (userBo.getOperationType().toLowerCase()) {
            case "add" -> handleAdd(userBo);
            case "update" -> handleUpdate(userBo);
            case "delete" -> handleDelete(userBo);
            default -> AjaxResult.error("不支持的操作类型：" + userBo.getOperationType() + "，请使用：add、update、delete");
        }, "用户操作");
    }

    private AjaxResult handleAdd(SysUserBo userBo) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);

        if (userBo.getDeptId() != null) {
            sysUser.setDeptId(userBo.getDeptId());
        } else if (userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
            List<SysDept> depts = getDeptsByName(userBo.getDeptName());
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            if (depts.size() > 1) {
                return formSelect("查询到 " + depts.size() + " 个部门，请选择所属部门：",
                    userBo, depts, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
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
        Long userId = userBo.getUserId();
        if (userId == null) {
            if (userBo.getUserName() == null || userBo.getUserName().isEmpty()) {
                return formMissing("修改操作必须传入用户名，请补充", userBo, "userName");
            }
            List<SysUser> users = getUsersByConditions(userBo);
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userBo.getUserName());
            }
            if (users.size() > 1) {
                return formSelect("查询到 " + users.size() + " 个用户，请选择要修改的用户：",
                    userBo, users, SysUser::getUserId, this::userLabel, "userId", "用户");
            }
            userId = users.getFirst().getUserId();
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);
        sysUser.setUserId(userId);

        if (userBo.getDeptId() != null) {
            sysUser.setDeptId(userBo.getDeptId());
        } else if (userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
            List<SysDept> depts = getDeptsByName(userBo.getDeptName());
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + userBo.getDeptName());
            }
            if (depts.size() > 1) {
                return formSelect("查询到 " + depts.size() + " 个部门，请选择所属部门：",
                    userBo, depts, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
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
                return formMissing("状态值不正确，请传入'有效'或'无效'", userBo, "status");
            }
        }

        R<Boolean> result = remoteUserService.updateUser(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDelete(SysUserBo userBo) {
        Long userId = userBo.getUserId();
        if (userId == null) {
            if (userBo.getUserName() == null || userBo.getUserName().isEmpty()) {
                return formMissing("删除操作必须传入用户名，请补充", userBo, "userName");
            }
            List<SysUser> users = getUsersByConditions(userBo);
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userBo.getUserName());
            }
            if (users.size() > 1) {
                return formSelect("查询到 " + users.size() + " 个用户，请选择要删除的用户：",
                    userBo, users, SysUser::getUserId, this::userLabel, "userId", "用户");
            }
            userId = users.getFirst().getUserId();
        }

        R<Boolean> result = remoteUserService.deleteUser(userId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "批量新增用户。参数：用户列表(userList)，每个元素需包含用户名、昵称等必填信息，可填写部门名称、手机号、邮箱、性别等。默认密码 admin123、状态有效。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddUsers(
            @ToolParam(description = "用户信息列表") List<SysUserBo> userList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(userList, "用户");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysUserBo userBo : userList) {
                AjaxResult validateResult = validate(userBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("userList", userList.stream().map(this::toMap).toList());
                return confirmResult("batchAddUsers", "add", "确认批量新增用户",
                    "即将批量新增 " + userList.size() + " 个用户，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下用户新增失败：");
            for (SysUserBo userBo : userList) {
                SysUser sysUser = new SysUser();
                BeanUtils.copyProperties(userBo, sysUser);

                Long deptId = userBo.getDeptId();
                if (deptId == null && userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
                    List<SysDept> depts = getDeptsByName(userBo.getDeptName());
                    if (depts.isEmpty()) {
                        batch.addFail(userBo.getUserName(), "部门不存在：" + userBo.getDeptName());
                        continue;
                    }
                    if (depts.size() > 1) {
                        batch.addFail(userBo.getUserName(), "部门名称匹配到多条记录，请携带部门ID");
                        continue;
                    }
                    deptId = depts.getFirst().getDeptId();
                }
                if (deptId != null) {
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
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(userBo.getUserName());
                } else {
                    batch.addFail(userBo.getUserName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增用户");
    }

    @Tool(description = "批量删除用户。参数：用户名列表(userNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteUsers(
            @ToolParam(description = "用户名列表，例如 [\"zhangsan\",\"lisi\"]") List<String> userNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(userNames, "用户");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("userNames", userNames);
                return confirmResult("batchDeleteUsers", "delete", "确认批量删除用户",
                    "即将批量删除 " + userNames.size() + " 个用户：" + joinNames(userNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下用户删除失败：");
            for (String userName : userNames) {
                List<SysUser> users = getUsersByName(userName);
                if (users.isEmpty()) {
                    batch.addFail(userName, "用户不存在");
                    continue;
                }
                if (users.size() > 1) {
                    batch.addFail(userName, "匹配到多条记录，请使用更精确的用户名或先查询用户ID");
                    continue;
                }
                R<Boolean> result = remoteUserService.deleteUser(users.getFirst().getUserId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(userName);
                } else {
                    batch.addFail(userName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除用户");
    }

    @Tool(description = "批量修改用户。参数：用户列表(userList)，每个元素需包含用户名(userName)或用户ID(userId)用于定位，另可填写昵称、手机号、邮箱、性别、状态、部门名称等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateUsers(
            @ToolParam(description = "用户信息列表") List<SysUserBo> userList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(userList, "用户");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("userList", userList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateUsers", "update", "确认批量修改用户",
                    "即将批量修改 " + userList.size() + " 个用户，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下用户修改失败：");
            for (SysUserBo userBo : userList) {
                String label = userBo.getUserName() != null && !userBo.getUserName().isEmpty()
                    ? userBo.getUserName() : "ID=" + userBo.getUserId();
                Long userId = userBo.getUserId();
                if (userId == null) {
                    if (userBo.getUserName() == null || userBo.getUserName().isEmpty()) {
                        batch.addFail(label, "缺少用户名或用户ID");
                        continue;
                    }
                    List<SysUser> users = getUsersByName(userBo.getUserName());
                    if (users.isEmpty()) {
                        batch.addFail(label, "用户不存在");
                        continue;
                    }
                    if (users.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带用户ID");
                        continue;
                    }
                    userId = users.getFirst().getUserId();
                }

                SysUser sysUser = new SysUser();
                BeanUtils.copyProperties(userBo, sysUser);
                sysUser.setUserId(userId);

                Long deptId = userBo.getDeptId();
                if (deptId == null && userBo.getDeptName() != null && !userBo.getDeptName().isEmpty()) {
                    List<SysDept> depts = getDeptsByName(userBo.getDeptName());
                    if (depts.isEmpty()) {
                        batch.addFail(label, "部门不存在：" + userBo.getDeptName());
                        continue;
                    }
                    if (depts.size() > 1) {
                        batch.addFail(label, "部门名称匹配到多条记录，请携带部门ID");
                        continue;
                    }
                    deptId = depts.getFirst().getDeptId();
                }
                if (deptId != null) {
                    sysUser.setDeptId(deptId);
                }

                if (userBo.getSex() != null && !userBo.getSex().isEmpty()) {
                    if ("男".equals(userBo.getSex())) {
                        sysUser.setSex("0");
                    } else if ("女".equals(userBo.getSex())) {
                        sysUser.setSex("1");
                    } else {
                        batch.addFail(label, "性别只能填写'男'或'女'");
                        continue;
                    }
                }

                if (userBo.getStatus() != null && !userBo.getStatus().isEmpty()) {
                    if ("有效".equals(userBo.getStatus()) || "启用".equals(userBo.getStatus())) {
                        sysUser.setStatus("0");
                    } else if ("无效".equals(userBo.getStatus()) || "停用".equals(userBo.getStatus())) {
                        sysUser.setStatus("1");
                    } else {
                        batch.addFail(label, "状态值不正确，请传入'有效'或'无效'");
                        continue;
                    }
                }

                R<Boolean> result = remoteUserService.updateUser(sysUser);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改用户");
    }

    @Tool(description = "为用户分配角色。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult userRoleAuth(UserRoleBo userRoleBo) {
        AjaxResult validateResult = validate(userRoleBo);
        if (validateResult != null) {
            return validateResult;
        }
        if (userRoleBo.getRoleNames() == null || userRoleBo.getRoleNames().length == 0) {
            return formMissing("请传入角色名称列表", userRoleBo, "roleNames");
        }
        if (!isConfirmed(userRoleBo)) {
            return confirmResult("userRoleAuth", "auth", "确认用户角色分配",
                "即将为用户「" + userRoleBo.getUserName() + "」分配角色，请确认是否继续。", toMap(userRoleBo));
        }
        return executeWithErrorHandling(() -> {
            Long userId = userRoleBo.getUserId();
            if (userId == null) {
                List<SysUser> users = getUsersByName(userRoleBo.getUserName());
                if (users.isEmpty()) {
                    return AjaxResult.error("用户不存在：" + userRoleBo.getUserName());
                }
                if (users.size() > 1) {
                    return formSelect("查询到 " + users.size() + " 个用户，请选择要分配角色的用户：",
                        userRoleBo, users, SysUser::getUserId, this::userLabel, "userId", "用户");
                }
                userId = users.getFirst().getUserId();
            }

            Long[] roleIds = new Long[userRoleBo.getRoleNames().length];
            for (int i = 0; i < userRoleBo.getRoleNames().length; i++) {
                String roleName = userRoleBo.getRoleNames()[i];
                List<SysRole> roles = getRolesByName(roleName);
                if (roles.isEmpty()) {
                    return AjaxResult.error("角色不存在：" + roleName);
                }
                if (roles.size() > 1) {
                    Long selectedRoleId = matchSelectedId(roles, userRoleBo.getRoleId(), SysRole::getRoleId);
                    if (selectedRoleId == null) {
                        return formSelect("查询到 " + roles.size() + " 个名称为「" + roleName + "」的角色，请选择要分配的角色：",
                            userRoleBo, roles, SysRole::getRoleId, this::roleLabel, "roleId", "角色");
                    }
                    roleIds[i] = selectedRoleId;
                } else {
                    roleIds[i] = roles.getFirst().getRoleId();
                }
            }
            R<Boolean> result = remoteUserService.authRole(userId, roleIds);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户角色分配成功") : AjaxResult.error(result.getMsg());
        }, "用户角色分配");
    }

    @Tool(description = "为用户分配岗位。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult userPostAuth(UserPostBo userPostBo) {
        AjaxResult validateResult = validate(userPostBo);
        if (validateResult != null) {
            return validateResult;
        }
        if (userPostBo.getPostNames() == null || userPostBo.getPostNames().length == 0) {
            return formMissing("请传入岗位名称列表", userPostBo, "postNames");
        }
        if (!isConfirmed(userPostBo)) {
            return confirmResult("userPostAuth", "auth", "确认用户岗位分配",
                "即将为用户「" + userPostBo.getUserName() + "」分配岗位，请确认是否继续。", toMap(userPostBo));
        }
        return executeWithErrorHandling(() -> {
            if (userPostBo.getUserId() == null) {
                List<SysUser> users = getUsersByName(userPostBo.getUserName());
                if (users.isEmpty()) {
                    return AjaxResult.error("用户不存在：" + userPostBo.getUserName());
                }
                if (users.size() > 1) {
                    return formSelect("查询到 " + users.size() + " 个用户，请选择要分配岗位的用户：",
                        userPostBo, users, SysUser::getUserId, this::userLabel, "userId", "用户");
                }
            }

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
                    errorMsg.append("查询到多个岗位，请补充更精确的岗位名称：").append(formatPostList(posts));
                }
            }
            if (!errorMsg.isEmpty()) {
                return AjaxResult.error(errorMsg.toString());
            }
            // 岗位分配需在用户编辑页面完成，当前对话渠道无法执行，不能向用户反馈成功
            return AjaxResult.error("岗位分配需在系统用户编辑页面操作，当前对话暂不支持直接分配");
        }, "用户岗位分配");
    }

    @Tool(description = "重置用户密码。参数：用户名(userName)和新密码(password)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult resetUserPassword(
            @ToolParam(description = "用户名") String userName,
            @ToolParam(description = "新密码") String password,
            @ToolParam(description = "用户ID，选择记录后自动回传，无需手动填写", required = false) Long userId,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("userName", userName);
        values.put("password", password);
        values.put("userId", userId);
        if (password == null || password.isEmpty()) {
            return formMissing("新密码不能为空，请补充", values, "password");
        }
        if (userId != null) {
            if (!Boolean.TRUE.equals(confirmed)) {
                return confirmResult("resetUserPassword", "update", "确认重置用户密码",
                    "即将重置所选用户的密码，请确认是否继续。", values);
            }
            return executeWithErrorHandling(() -> resetPasswordById(userId, password), "重置用户密码");
        }
        if (userName == null || userName.isEmpty()) {
            return formMissing("用户名不能为空，请补充", values, "userName");
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            return confirmResult("resetUserPassword", "update", "确认重置用户密码",
                "即将重置用户「" + userName + "」的密码，请确认是否继续。", values);
        }
        return executeWithErrorHandling(() -> {
            List<SysUser> users = getUsersByName(userName);
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userName);
            }
            if (users.size() > 1) {
                return formSelect("查询到 " + users.size() + " 个用户，请选择要重置密码的用户：",
                    values, users, SysUser::getUserId, this::userLabel, "userId", "用户");
            }
            return resetPasswordById(users.getFirst().getUserId(), password);
        }, "重置用户密码");
    }

    private AjaxResult resetPasswordById(Long userId, String password) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setPassword(password);
        R<Boolean> result = remoteUserService.resetPwd(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("密码重置成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "修改用户状态（启用/停用）。参数：用户名(userName)和状态(status: 有效/启用 或 无效/停用)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult changeUserStatus(
            @ToolParam(description = "用户名") String userName,
            @ToolParam(description = "状态：有效/启用 或 无效/停用") String status,
            @ToolParam(description = "用户ID，选择记录后自动回传，无需手动填写", required = false) Long userId,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("userName", userName);
        values.put("status", status);
        values.put("userId", userId);
        if (status == null || status.isEmpty()) {
            return formMissing("状态不能为空，请传入'有效'或'无效'", values, "status");
        }
        if (!"有效".equals(status) && !"启用".equals(status) && !"无效".equals(status) && !"停用".equals(status)) {
            return formMissing("状态值不正确，请传入'有效'或'无效'", values, "status");
        }
        if (userId != null) {
            if (!Boolean.TRUE.equals(confirmed)) {
                return confirmResult("changeUserStatus", "update", "确认修改用户状态",
                    "即将" + ("有效".equals(status) || "启用".equals(status) ? "启用" : "停用") + "所选用户，请确认是否继续。", values);
            }
            return executeWithErrorHandling(() -> changeStatusById(userId, status), "修改用户状态");
        }
        if (userName == null || userName.isEmpty()) {
            return formMissing("用户名不能为空，请补充", values, "userName");
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            return confirmResult("changeUserStatus", "update", "确认修改用户状态",
                "即将" + ("有效".equals(status) || "启用".equals(status) ? "启用" : "停用") + "用户「" + userName + "」，请确认是否继续。", values);
        }
        return executeWithErrorHandling(() -> {
            List<SysUser> users = getUsersByName(userName);
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userName);
            }
            if (users.size() > 1) {
                return formSelect("查询到 " + users.size() + " 个用户，请选择要修改状态的用户：",
                    values, users, SysUser::getUserId, this::userLabel, "userId", "用户");
            }
            return changeStatusById(users.getFirst().getUserId(), status);
        }, "修改用户状态");
    }

    private AjaxResult changeStatusById(Long userId, String status) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        if ("有效".equals(status) || "启用".equals(status)) {
            sysUser.setStatus("0");
        } else {
            sysUser.setStatus("1");
        }
        R<Boolean> result = remoteUserService.changeStatus(sysUser);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户状态修改成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "修改用户所属部门。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult userDeptAuth(UserDeptBo userDeptBo) {
        AjaxResult validateResult = validate(userDeptBo);
        if (validateResult != null) {
            return validateResult;
        }
        Long userId = userDeptBo.getUserId();
        if (userId == null) {
            if (userDeptBo.getUserName() == null || userDeptBo.getUserName().isEmpty()) {
                return formMissing("请传入用户名", userDeptBo, "userName");
            }
            List<SysUser> users = getUsersByName(userDeptBo.getUserName());
            if (users.isEmpty()) {
                return AjaxResult.error("用户不存在：" + userDeptBo.getUserName());
            }
            if (users.size() > 1) {
                return formSelect("查询到 " + users.size() + " 个用户，请选择要调整部门的用户：",
                    userDeptBo, users, SysUser::getUserId, this::userLabel, "userId", "用户");
            }
            userId = users.getFirst().getUserId();
        }

        Long deptId = userDeptBo.getDeptId();
        if (deptId == null) {
            if (userDeptBo.getDeptName() == null || userDeptBo.getDeptName().isEmpty()) {
                return formMissing("请传入部门名称", userDeptBo, "deptName");
            }
            List<SysDept> depts = getDeptsByName(userDeptBo.getDeptName());
            if (depts.isEmpty()) {
                return AjaxResult.error("部门不存在：" + userDeptBo.getDeptName());
            }
            if (depts.size() > 1) {
                return formSelect("查询到 " + depts.size() + " 个部门，请选择要调整到的部门：",
                    userDeptBo, depts, SysDept::getDeptId, this::deptLabel, "deptId", "部门");
            }
            deptId = depts.getFirst().getDeptId();
        }

        if (!isConfirmed(userDeptBo)) {
            return confirmResult("userDeptAuth", "update", "确认修改用户所属部门",
                "即将把用户「" + userDeptBo.getUserName() + "」调整到部门「" + userDeptBo.getDeptName() + "」，请确认是否继续。", toMap(userDeptBo));
        }

        Long finalUserId = userId;
        Long finalDeptId = deptId;
        return executeWithErrorHandling(() -> {
            SysUser sysUser = new SysUser();
            sysUser.setUserId(finalUserId);
            sysUser.setDeptId(finalDeptId);
            R<Boolean> result = remoteUserService.updateUser(sysUser);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户部门修改成功") : AjaxResult.error(result.getMsg());
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

    private String userLabel(SysUser u) {
        return "用户名：" + u.getUserName()
            + "，昵称：" + u.getNickName()
            + "，手机：" + (u.getPhonenumber() != null ? u.getPhonenumber() : "无")
            + "，邮箱：" + (u.getEmail() != null ? u.getEmail() : "无");
    }

    private String deptLabel(SysDept d) {
        return "部门名称：" + d.getDeptName()
            + "，负责人：" + (d.getLeader() != null ? d.getLeader() : "无")
            + "，联系电话：" + (d.getPhone() != null ? d.getPhone() : "无");
    }

    private String roleLabel(SysRole r) {
        return "角色名称：" + r.getRoleName()
            + "，权限字符：" + r.getRoleKey()
            + "，状态：" + ("0".equals(r.getStatus()) ? "正常" : "停用");
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
