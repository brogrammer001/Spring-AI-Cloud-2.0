package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteMenuService;
import com.mall.system.api.domain.SysMenuVo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenMenuToolServiceImpl implements BaseToolService {

    @Autowired
    private RemoteMenuService remoteMenuService;

    @Tool(description = "菜单导航工具。输入：菜单名称。功能：获取菜单组件路径以供前端跳转。 [JSON]")
    public AjaxResult getMenu(@ToolParam(description = "菜单名称") String menuName) {
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
}
