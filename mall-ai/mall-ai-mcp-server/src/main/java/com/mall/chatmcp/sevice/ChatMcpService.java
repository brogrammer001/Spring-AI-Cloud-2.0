package com.mall.chatmcp.sevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.chatmcp.bo.SysUserBo;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteMenuService;
import com.mall.system.api.RemoteUserService;
import com.mall.system.api.domain.ChatEventType;
import com.mall.system.api.domain.ChatStreamResponse;
import com.mall.system.api.domain.SysMenuVo;
import com.mall.system.api.domain.SysUser;
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
    private Validator validator; // 注入 Spring 校验器

    private ObjectMapper objectMapper = new ObjectMapper();

    @Tool(description = "【菜单跳转工具-强制调用】" +
        "触发条件：用户意图为打开、跳转、查看某个菜单页面时。" +
        "输入：菜单名称字符串（注意：如果用户说'XX菜单'，请自动剔除'菜单'二字，只传XX）。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若查询成功：你的回复必须是且仅是方法返回的 JSON 字符串（例如：{\"type\":\"OPEN_MENU\", \"content\":\"system/user/index\", \"data\":null}），不要加json 标记。" +
        "- 若查询失败（未找到或多个）：你的回复必须是且仅是方法返回的 JSON 字符串（type 为 ERROR）。")
    public String getMenu(@ToolParam(description = "要查询的菜单名称，例如：用户、部门") String menuName) {
        SysMenuVo menu = new SysMenuVo();
        menu.setMenuName(menuName);
        menu.setMenuType("C");
        R<List<SysMenuVo>> menuResult = remoteMenuService.getMenuByParam(menu);

        if (menuResult.getCode() == 200) {
            List<SysMenuVo> list = menuResult.getData();
            if (list.isEmpty()) {
                return buildJsonResponse(ChatEventType.ERROR, "抱歉，未查询到对应菜单", null);
            }
            if (list.size() > 1) {
                return buildJsonResponse(ChatEventType.ERROR, "抱歉，查询出多个对应菜单，请详细说明", null);
            }
            // 成功：返回 OPEN_MENU 类型，content 为组件路径
            return buildJsonResponse(ChatEventType.OPEN_MENU, list.getFirst().getComponent(), null);
        }
        return buildJsonResponse(ChatEventType.ERROR, "抱歉，未查询到对应菜单", null);
    }

    @Tool(description = "【数据新增工具-强制调用】" +
        "触发条件：用户要求新增/创建/添加数据，且话语中携带了至少一个具体字段值。" +
        "参数提取规范：从用户话语中提取信息填入 SysUserBo，未提及的属性设为 null。" +
        "返回值处理规范（唯一正确映射，禁止篡改）：" +
        "- 本方法返回一个 JSON 字符串。" +
        "- 若本方法返回 type 为 'CONFIRM' 的 JSON：你的回复必须是且仅是该 JSON 字符串（例如 {\"type\":\"CONFIRM\", \"content\":\"新增成功\", ...}）。" +
        "- 若本方法返回 type 为 'ERROR' 的 JSON：你的回复必须是且仅是该 JSON 字符串（例如 {\"type\":\"ERROR\", \"content\":\"新增失败...\", ...}）。" +
        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
    public String addUser(SysUserBo userBo) {
        // 1. 手动触发校验
        BindingResult bindingResult = new BeanPropertyBindingResult(userBo, "sysUserBo");
        validator.validate(userBo, bindingResult);

        // 2. 校验失败处理
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return buildJsonResponse(ChatEventType.ERROR, errorMsg.toString(), null);
        }

        try {
            // 3. 调用微服务新增接口
            SysUser sysUser = new SysUser();
            BeanUtils.copyProperties(userBo, sysUser);
            R<Boolean> result = remoteUserService.addUser(sysUser);

            // 4. 结果处理
            if (result.getCode() == 200 && result.getData()) {
                // 成功：返回 CONFIRM 类型，表示操作已确认完成
                return buildJsonResponse(ChatEventType.CONFIRM, "新增成功", null);
            } else {
                // 业务报错
                return buildJsonResponse(ChatEventType.ERROR, result.getMsg(), null);
            }
        } catch (Exception e) {
            // 系统级异常
            return buildJsonResponse(ChatEventType.ERROR, "系统内部异常，新增失败，请稍后再试。", null);
        }
    }


//    @Tool(description = "【数据删除工具-强制调用】" +
//        "触发条件：用户要求删除/移除某个用户数据。" +
//        "输入参数：要删除用户的 ID。" +
//        "返回值处理规范（唯一正确映射，禁止篡改）：" +
//        "- 本方法返回一个 JSON 字符串。" +
//        "- 若删除成功：你的回复必须是且仅是该 JSON 字符串（type 为 CONFIRM）。" +
//        "- 若删除失败：你的回复必须是且仅是该 JSON 字符串（type 为 ERROR）。" +
//        "- 【绝对禁止】添加任何 Markdown 代码块标记（json），直接返回原始 JSON 文本。")
//    public String deleteUser(@ToolParam(description = "要删除的用户ID（数字）") Long userId) {
//        if (userId == null) {
//            return buildJsonResponse(ChatEventType.ERROR, "删除失败，原因：用户ID不能为空", null);
//        }
//
//        try {
//            // 假设 RemoteUserService 有 deleteUser 方法
//            R<Boolean> result = remoteUserService.deleteUser(userId);
//
//            if (result.getCode() == 200 && result.getData()) {
//                return buildJsonResponse(ChatEventType.CONFIRM, "删除成功", null);
//            } else {
//                return buildJsonResponse(ChatEventType.ERROR, "删除失败：" + result.getMsg(), null);
//            }
//        } catch (Exception e) {
//            return buildJsonResponse(ChatEventType.ERROR, "系统内部异常，删除失败，请稍后再试。", null);
//        }
//    }
//
//    @Tool(description = "【数据修改工具-强制调用】" +
//        "触发条件：用户要求修改/编辑/更新某个用户的信息。" +
//        "输入参数：用户ID 以及需要修改的字段信息。" +
//        "参数提取规范：从用户话语中提取信息填入 SysUserBo，未提及的属性设为 null。" +
//        "返回值处理规范（唯一正确映射，禁止篡改）：" +
//        "- 本方法返回一个 JSON 字符串。" +
//        "- 若修改成功：你的回复必须是且仅是该 JSON 字符串（type 为 CONFIRM）。" +
//        "- 若修改失败（含校验失败）：你的回复必须是且仅是该 JSON 字符串（type 为 ERROR）。" +
//        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
//    public String updateUser(@ToolParam(description = "要修改的用户ID（数字）") Long userId, SysUserBo userBo) {
//        // 1. 校验 ID
//        if (userId == null) {
//            return buildJsonResponse(ChatEventType.ERROR, "修改失败，原因：用户ID不能为空", null);
//        }
//
//        // 2. 校验实体数据 (注意：这里会校验 SysUserBo 里的 @NotBlank，如果业务允许部分更新，建议调整校验策略或分组校验)
//        BindingResult bindingResult = new BeanPropertyBindingResult(userBo, "sysUserBo");
//        validator.validate(userBo, bindingResult);
//
//        if (bindingResult.hasErrors()) {
//            StringBuilder errorMsg = new StringBuilder("修改失败，原因：");
//            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
//            return buildJsonResponse(ChatEventType.ERROR, errorMsg.toString(), null);
//        }
//
//        try {
//            // 3. 转换并设置 ID
//            SysUser sysUser = new SysUser();
//            BeanUtils.copyProperties(userBo, sysUser);
//            sysUser.setUserId(userId); // 强制设置 ID
//
//            // 假设 RemoteUserService 有 editUser 或 updateUser 方法
//            R<Boolean> result = remoteUserService.editUser(sysUser);
//
//            if (result.getCode() == 200) {
//                return buildJsonResponse(ChatEventType.CONFIRM, "修改成功", null);
//            } else {
//                return buildJsonResponse(ChatEventType.ERROR, "修改失败：" + result.getMsg(), null);
//            }
//        } catch (Exception e) {
//            return buildJsonResponse(ChatEventType.ERROR, "系统内部异常，修改失败，请稍后再试。", null);
//        }
//    }
//
//    @Tool(description = "【数据查询工具-强制调用】" +
//        "触发条件：用户要求查询/搜索/查看用户列表。" +
//        "输入参数：根据用户提供的条件填入 SysUserBo（如部门名称、昵称等），未提及的条件可不填。" +
//        "返回值处理规范（唯一正确映射，禁止篡改）：" +
//        "- 本方法返回一个 JSON 字符串。" +
//        "- 若查询成功：你的回复必须是且仅是 type 为 DATA_TABLE 的 JSON 字符串，data 字段包含列表数据。" +
//        "- 若查询失败：你的回复必须是且仅是 type 为 ERROR 的 JSON 字符串。" +
//        "- 【绝对禁止】添加任何 Markdown 代码块标记（```json），直接返回原始 JSON 文本。")
//    public String queryUser(SysUserBo userBo) {
//        // 注意：查询场景下通常不需要强校验 @NotBlank，否则用户只查部门不填账号会报错。
//        // 因此这里手动调用 Validator 之前可以加判断，或者直接不调用 Validator。
//        // 鉴于 SysUserBo 中 userName 是 @NotBlank，这里选择跳过 Validator.validate 以支持模糊查询。
//
//        try {
//            // 假设 RemoteUserService 有 listUsers 或 selectUserList 方法
//            // 这里假设返回的是 R<List<SysUserVo>> 或者包含 List 的结构
//            R<List<SysUserVo>> result = remoteUserService.listUsers(userBo);
//
//            if (result.getCode() == 200 && result.getData() != null) {
//                // 成功：返回 DATA_TABLE 类型，data 放入列表，content 放入提示语
//                return buildJsonResponse(ChatEventType.DATA_TABLE, "查询成功", result.getData());
//            } else {
//                return buildJsonResponse(ChatEventType.ERROR, "未查询到数据或查询失败：" + result.getMsg(), null);
//            }
//        } catch (Exception e) {
//            return buildJsonResponse(ChatEventType.ERROR, "系统内部异常，查询失败，请稍后再试。", null);
//        }
//    }

    /**
     * 辅助方法：构建统一的 JSON 响应字符串
     */
    private String buildJsonResponse(ChatEventType type, String content, Object data) {
        try {
            // 这里的 ChatStreamResponse 和 ChatEventType 是你提供的类
            ChatStreamResponse response = new ChatStreamResponse(type, content, data);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            // 容错处理，防止 JSON 序列化失败导致大模型理解错误
            return String.format("{\"type\":\"ERROR\",\"content\":\"系统序列化异常:%s\",\"data\":null}", e.getMessage());
        }
    }
}
