package com.mall.chatmcp.sevice;

import com.mall.chatmcp.bo.*;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.*;
import com.mall.system.api.domain.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class ChatMcpService {

    @Autowired
    private RemoteMenuService remoteMenuService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteRoleService remoteRoleService;

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Autowired
    private RemoteNoticeService remoteNoticeService;

    @Autowired
    private RemotePostService remotePostService;

    @Autowired
    private Validator validator;

    @Tool(description = """
           菜单跳转工具。
           触发条件：当用户的意图是打开、跳转、查看任何菜单或页面时，【必须】调用此工具。
           输出要求：
           1. 如果查询成功，工具将返回 JSON 数据，请将此 JSON 字符串作为你的【唯一回复】，原样透传，禁止添加任何额外文字。
        """)
    public AjaxResult getMenu(@ToolParam(description = "核心菜单名称。如果用户说'用户菜单'，请自动剔除'菜单'二字，只传入'用户'") String menuName) {
        SysMenuVo menu = new SysMenuVo();
        menu.setMenuName(menuName);
        menu.setMenuType("C");
        R<List<SysMenuVo>> menuResult = remoteMenuService.getMenuByParam(menu);

        if (menuResult.getCode() == 200) {
            List<SysMenuVo> list = menuResult.getData();
            if (list.isEmpty()) {
                return AjaxResult.error("抱歉，未查询到对应菜单");
            }
            if (list.size() > 1) {
                return AjaxResult.error("抱歉，查询出多个对应菜单，请详细说明");
            }
            // 成功：返回 OPEN_MENU 类型，content 为组件路径
            return new AjaxResult(8001, "已为您打开" + list.getFirst().getMenuName() + "菜单。", list.getFirst().getComponent());
        }
        return AjaxResult.error("抱歉，未查询到对应菜单");
    }

    @Tool(description = "【用户数据CRUD工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加/修改/更新/删除/查询用户数据。" +
        "参数提取规范：" +
        "- operationType：根据用户意图设置操作类型，add-新增，update-修改，delete-删除，query-查询。" +
        "- userId：修改和删除操作时必须传入用户ID。" +
        "- 其他字段：从用户话语中提取信息填入 SysUserBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'SUCCESS' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult userCrud(SysUserBo userBo) {
        String operationType = userBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、query");
        }

        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleAdd(userBo);
                case "update" -> handleUpdate(userBo);
                case "delete" -> handleDelete(userBo);
                case "query" -> handleQuery(userBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、query");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handleAdd(SysUserBo userBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(userBo, "sysUserBo");
        validator.validate(userBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);
        R<Boolean> result = remoteUserService.addUserApi(sysUser);

        if (result.getCode() == 200 && result.getData()) {
            return AjaxResult.success("新增成功");
        } else {
            return AjaxResult.error(result.getMsg());
        }
    }

    private AjaxResult handleUpdate(SysUserBo userBo) {
        if (userBo.getUserId() == null) {
            return AjaxResult.error("修改操作必须传入用户ID");
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);
        R<Boolean> result = remoteUserService.updateUser(sysUser);

        if (result.getCode() == 200 && result.getData()) {
            return AjaxResult.success("修改成功");
        } else {
            return AjaxResult.error(result.getMsg());
        }
    }

    private AjaxResult handleDelete(SysUserBo userBo) {
        if (userBo.getUserId() == null) {
            return AjaxResult.error("删除操作必须传入用户ID");
        }

        R<Boolean> result = remoteUserService.deleteUser(userBo.getUserId());

        if (result.getCode() == 200 && result.getData()) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error(result.getMsg());
        }
    }

    private AjaxResult handleQuery(SysUserBo userBo) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(userBo, sysUser);
        R<List<SysUser>> result = remoteUserService.getUserList(sysUser);

        if (result.getCode() == 200) {
            List<SysUser> users = result.getData();
            if (users.isEmpty()) {
                return AjaxResult.error("未查询到用户数据");
            }
            return AjaxResult.success("查询成功", users);
        } else {
            return AjaxResult.error(result.getMsg());
        }
    }

    @Tool(description = "【角色数据CRUD工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加/修改/更新/删除/查询角色数据。" +
        "参数提取规范：" +
        "- operationType：根据用户意图设置操作类型，add-新增，update-修改，delete-删除，query-查询。" +
        "- roleId：修改和删除操作时必须传入角色ID。" +
        "- 其他字段：从用户话语中提取信息填入 SysRoleBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult roleCrud(SysRoleBo roleBo) {
        String operationType = roleBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、query");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleRoleAdd(roleBo);
                case "update" -> handleRoleUpdate(roleBo);
                case "delete" -> handleRoleDelete(roleBo);
                case "query" -> handleRoleQuery(roleBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、query");
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

    private AjaxResult handleRoleQuery(SysRoleBo roleBo) {
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(roleBo, sysRole);
        R<List<SysRole>> result = remoteRoleService.getRoleList(sysRole);
        if (result.getCode() == 200) {
            List<SysRole> roles = result.getData();
            return roles.isEmpty() ? AjaxResult.error("未查询到角色数据") : AjaxResult.success("查询成功", roles);
        }
        return AjaxResult.error(result.getMsg());
    }

    @Tool(description = "【部门数据CRUD工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加/修改/更新/删除/查询部门数据。" +
        "参数提取规范：" +
        "- operationType：根据用户意图设置操作类型，add-新增，update-修改，delete-删除，query-查询。" +
        "- deptId：修改和删除操作时必须传入部门ID。" +
        "- 其他字段：从用户话语中提取信息填入 SysDeptBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult deptCrud(SysDeptBo deptBo) {
        String operationType = deptBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、query");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleDeptAdd(deptBo);
                case "update" -> handleDeptUpdate(deptBo);
                case "delete" -> handleDeptDelete(deptBo);
                case "query" -> handleDeptQuery(deptBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、query");
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
        R<Boolean> result = remoteDeptService.addDept(sysDept);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDeptUpdate(SysDeptBo deptBo) {
        if (deptBo.getDeptId() == null) {
            return AjaxResult.error("修改操作必须传入部门ID");
        }
        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);
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

    private AjaxResult handleDeptQuery(SysDeptBo deptBo) {
        SysDept sysDept = new SysDept();
        BeanUtils.copyProperties(deptBo, sysDept);
        R<List<SysDept>> result = remoteDeptService.getDeptList(sysDept);
        if (result.getCode() == 200) {
            List<SysDept> depts = result.getData();
            return depts.isEmpty() ? AjaxResult.error("未查询到部门数据") : AjaxResult.success("查询成功", depts);
        }
        return AjaxResult.error(result.getMsg());
    }

    @Tool(description = "【通知公告CRUD工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加/修改/更新/删除/查询通知公告数据。" +
        "参数提取规范：" +
        "- operationType：根据用户意图设置操作类型，add-新增，update-修改，delete-删除，query-查询。" +
        "- noticeId：修改和删除操作时必须传入公告ID。" +
        "- 其他字段：从用户话语中提取信息填入 SysNoticeBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult noticeCrud(SysNoticeBo noticeBo) {
        String operationType = noticeBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、query");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleNoticeAdd(noticeBo);
                case "update" -> handleNoticeUpdate(noticeBo);
                case "delete" -> handleNoticeDelete(noticeBo);
                case "query" -> handleNoticeQuery(noticeBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、query");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handleNoticeAdd(SysNoticeBo noticeBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(noticeBo, "sysNoticeBo");
        validator.validate(noticeBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.addNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeUpdate(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeId() == null) {
            return AjaxResult.error("修改操作必须传入公告ID");
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeDelete(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeId() == null) {
            return AjaxResult.error("删除操作必须传入公告ID");
        }
        R<Boolean> result = remoteNoticeService.deleteNotice(noticeBo.getNoticeId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeQuery(SysNoticeBo noticeBo) {
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<List<SysNotice>> result = remoteNoticeService.getNoticeList(sysNotice);
        if (result.getCode() == 200) {
            List<SysNotice> notices = result.getData();
            return notices.isEmpty() ? AjaxResult.error("未查询到公告数据") : AjaxResult.success("查询成功", notices);
        }
        return AjaxResult.error(result.getMsg());
    }

    @Tool(description = "【岗位数据CRUD工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加/修改/更新/删除/查询岗位数据。" +
        "参数提取规范：" +
        "- operationType：根据用户意图设置操作类型，add-新增，update-修改，delete-删除，query-查询。" +
        "- postId：修改和删除操作时必须传入岗位ID。" +
        "- 其他字段：从用户话语中提取信息填入 SysPostBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult postCrud(SysPostBo postBo) {
        String operationType = postBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、query");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handlePostAdd(postBo);
                case "update" -> handlePostUpdate(postBo);
                case "delete" -> handlePostDelete(postBo);
                case "query" -> handlePostQuery(postBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、query");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handlePostAdd(SysPostBo postBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(postBo, "sysPostBo");
        validator.validate(postBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.addPost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostUpdate(SysPostBo postBo) {
        if (postBo.getPostId() == null) {
            return AjaxResult.error("修改操作必须传入岗位ID");
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.updatePost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostDelete(SysPostBo postBo) {
        if (postBo.getPostId() == null) {
            return AjaxResult.error("删除操作必须传入岗位ID");
        }
        R<Boolean> result = remotePostService.deletePost(postBo.getPostId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostQuery(SysPostBo postBo) {
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<List<SysPost>> result = remotePostService.getPostList(sysPost);
        if (result.getCode() == 200) {
            List<SysPost> posts = result.getData();
            return posts.isEmpty() ? AjaxResult.error("未查询到岗位数据") : AjaxResult.success("查询成功", posts);
        }
        return AjaxResult.error(result.getMsg());
    }

    @Tool(description = "【角色和部门关联工具-强制调用】" +
        "触发条件：用户要求为角色分配数据权限范围（部门）。" +
        "参数提取规范：" +
        "- roleId：角色ID。" +
        "- deptIds：部门ID列表，用于设置角色的数据权限范围。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult roleDeptAuth(RoleDeptBo roleDeptBo) {
        if (roleDeptBo.getRoleId() == null) {
            return AjaxResult.error("角色ID不能为空");
        }
        try {
            SysRole sysRole = new SysRole();
            sysRole.setRoleId(roleDeptBo.getRoleId());
            sysRole.setDeptIds(roleDeptBo.getDeptIds());
            R<Boolean> result = remoteRoleService.authDataScope(sysRole);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("角色数据权限分配成功") : AjaxResult.error(result.getMsg());
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，角色数据权限分配失败，请稍后再试。");
        }
    }

    @Tool(description = "【角色和菜单关联工具-强制调用】" +
        "触发条件：用户要求查询角色已分配的菜单权限。" +
        "参数提取规范：" +
        "- roleId：角色ID。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'SUCCESS' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult getRoleMenuAuth(RoleMenuBo roleMenuBo) {
        if (roleMenuBo.getRoleId() == null) {
            return AjaxResult.error("角色ID不能为空");
        }
        try {
            R<List<Long>> result = remoteRoleService.getRoleMenuIds(roleMenuBo.getRoleId());
            if (result.getCode() == 200) {
                List<Long> menuIds = result.getData();
                return menuIds == null || menuIds.isEmpty() ? AjaxResult.error("该角色未分配菜单权限") : AjaxResult.success("查询成功", menuIds);
            }
            return AjaxResult.error(result.getMsg());
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，查询角色菜单权限失败，请稍后再试。");
        }
    }

    @Tool(description = "【用户和角色关联工具-强制调用】" +
        "触发条件：用户要求为用户分配角色或查询用户已分配的角色。" +
        "参数提取规范：" +
        "- userId：用户ID。" +
        "- roleIds：角色ID列表，用于为用户分配角色（可选，不传则查询已有角色）。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'SUCCESS' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult userRoleAuth(UserRoleBo userRoleBo) {
        if (userRoleBo.getUserId() == null) {
            return AjaxResult.error("用户ID不能为空");
        }
        try {
            if (userRoleBo.getRoleIds() != null && userRoleBo.getRoleIds().length > 0) {
                R<Boolean> result = remoteUserService.authRole(userRoleBo.getUserId(), userRoleBo.getRoleIds());
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户角色分配成功") : AjaxResult.error(result.getMsg());
            } else {
                R<List<Long>> result = remoteUserService.getUserRoleIds(userRoleBo.getUserId());
                if (result.getCode() == 200) {
                    List<Long> roleIds = result.getData();
                    return roleIds == null || roleIds.isEmpty() ? AjaxResult.error("该用户未分配角色") : AjaxResult.success("查询成功", roleIds);
                }
                return AjaxResult.error(result.getMsg());
            }
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，用户角色分配失败，请稍后再试。");
        }
    }

    @Tool(description = "【用户和岗位关联工具-强制调用】" +
        "触发条件：用户要求查询用户已分配的岗位。" +
        "参数提取规范：" +
        "- userId：用户ID。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'SUCCESS' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult getUserPostAuth(UserPostBo userPostBo) {
        if (userPostBo.getUserId() == null) {
            return AjaxResult.error("用户ID不能为空");
        }
        try {
            R<List<Long>> result = remoteUserService.getUserPostIds(userPostBo.getUserId());
            if (result.getCode() == 200) {
                List<Long> postIds = result.getData();
                return postIds == null || postIds.isEmpty() ? AjaxResult.error("该用户未分配岗位") : AjaxResult.success("查询成功", postIds);
            }
            return AjaxResult.error(result.getMsg());
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，查询用户岗位失败，请稍后再试。");
        }
    }

    @Tool(description = "【用户和部门关联工具-强制调用】" +
        "触发条件：用户要求查询用户所属部门或修改用户所属部门。" +
        "参数提取规范：" +
        "- userId：用户ID。" +
        "- deptId：部门ID，用于修改用户所属部门（可选，不传则查询当前部门）。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'SUCCESS' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public AjaxResult userDeptAuth(UserDeptBo userDeptBo) {
        if (userDeptBo.getUserId() == null) {
            return AjaxResult.error("用户ID不能为空");
        }
        try {
            if (userDeptBo.getDeptId() != null) {
                SysUser sysUser = new SysUser();
                sysUser.setUserId(userDeptBo.getUserId());
                sysUser.setDeptId(userDeptBo.getDeptId());
                R<Boolean> result = remoteUserService.updateUser(sysUser);
                return result.getCode() == 200 && result.getData() ? AjaxResult.success("用户部门修改成功") : AjaxResult.error(result.getMsg());
            } else {
                R<SysUser> result = remoteUserService.getUserById(userDeptBo.getUserId());
                if (result.getCode() == 200) {
                    SysUser user = result.getData();
                    if (user == null) {
                        return AjaxResult.error("用户不存在");
                    }
                    return AjaxResult.success("查询成功", user.getDeptId());
                }
                return AjaxResult.error(result.getMsg());
            }
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，用户部门操作失败，请稍后再试。");
        }
    }

}
