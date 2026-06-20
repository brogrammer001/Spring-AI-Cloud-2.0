package com.mall.chatmcp.sevice;

import com.mall.chatmcp.bo.SysUserBo;
import com.mall.common.core.domain.R;
//import com.mall.system.api.domain.SysMenuVo;
//import com.mall.system.api.domain.SysUser;
//import com.mall.system.api.feign.RemoteUserService;
//import com.mall.system.api.feign.openpage.RemoteOpenPageService;
import com.mall.system.api.RemoteUserService;
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

//    @Autowired
//    private RemoteOpenPageService remoteOpenPageService;
//
//    @Autowired
//    private RemoteUserService remoteUserService;
//
//    @Autowired
//    private Validator validator; // 注入 Spring 校验器
//
//    @Tool(description = "根据用户输入的菜单名称，查询系统对应的组件路径。" +
//        "输入：菜单名称字符串（注意：如果用户说'XX菜单'，请自动剔除'菜单'二字，只传XX）。" +
//        "输出：返回组件路径字符串（如 system/user/index），或者未找到/找到多个的错误提示文本。")
//    public String getMenu(@ToolParam(description = "要查询的菜单名称，例如：用户、部门") String menuName) {
//        SysMenuVo menu = new SysMenuVo();
//        menu.setMenuName(menuName);
//        menu.setMenuType("C");
//        R<List<SysMenuVo>> menuResult = remoteOpenPageService.getMenuByParam(menu);
//        if (menuResult.getCode() == 200) {
//            List<SysMenuVo> list = menuResult.getData();
//            if (list.isEmpty()) return "抱歉，未查询到对应菜单";
//
//            if (list.size() > 1) return "抱歉，查询出多个对应菜单，请详细说明";
//
//            return list.get(0).getComponent();
//        }
//        return "抱歉，未查询到对应菜单";
//    }
//
//    @Tool(description = "【数据新增工具-强制调用】" +
//        "触发条件：用户要求新增/创建/添加数据，且话语中携带了至少一个具体字段值（如账号、姓名、部门等）。" +
//        "【必须调用本工具，不可跳过，不可预判结果】" +
//        "参数提取规范：从用户话语中提取信息填入 SysUserBo。未提及的属性设为 null，【绝对不要因为缺字段就拒绝调用，让后端校验】！" +
//        "返回值处理规范（唯一正确映射，禁止篡改）：" +
//        "- 若本方法返回字符串 '新增成功'：你的回复必须是且仅是 [[2001:新增成功]]" +
//        "- 若本方法返回任何其他字符串（如'新增失败，原因：...'或'账号已存在'等）：你的回复必须是且仅是该字符串原文，禁止加中括号，禁止修改内容")
//    public String addUser(SysUserBo userBo) {
//        // 1. 手动触发校验
//        BindingResult bindingResult = new BeanPropertyBindingResult(userBo, "sysUserBo");
//        validator.validate(userBo, bindingResult);
//
//        // 2. 如果校验失败，把错误信息拼起来，直接返回给大模型
//        if (bindingResult.hasErrors()) {
//            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
//            bindingResult.getFieldErrors().forEach(error -> {
//                // 把 "用户账号不能为空" 这种信息提取出来
//                errorMsg.append(error.getDefaultMessage()).append("；");
//            });
//            // 返回纯文本，AI 会把这个原因告诉用户
//            return errorMsg.toString();
//        }
//
//        try {
//            // 2. 调用微服务新增接口
//            SysUser sysUser = new SysUser();
//            BeanUtils.copyProperties(userBo, sysUser);
//            R<Boolean> result = remoteUserService.addUser(sysUser);
//
//            // 3. 根据结果返回（切记：成功就返回纯文本“新增成功”，不要包 R<T>）
//            if (result.getCode() == 200 && result.getData()) {
//                return "新增成功";
//            } else {
//                // 业务报错（比如账号重复），把后端的 msg 原样抛给 AI
//                return result.getMsg();
//            }
//        } catch (Exception e) {
//            // 只有系统级异常（网络超时等）才记录日志并返回模糊提示
//            return "系统内部异常，新增失败，请稍后再试。";
//        }
//    }
}
