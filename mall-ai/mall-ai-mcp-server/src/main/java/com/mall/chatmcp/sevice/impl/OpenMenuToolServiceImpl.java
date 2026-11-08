package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteMenuService;
import com.mall.system.api.domain.SysMenuVo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenMenuToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteMenuService remoteMenuService;

    @Tool(description = "菜单导航工具。输入：菜单名称。功能：获取菜单组件路径以供前端跳转。 [JSON]", returnDirect = true)
    public AjaxResult getMenu(
            @ToolParam(description = "菜单名称", required = false) String menuName,
            @ToolParam(description = "菜单ID，选择菜单后自动回传，无需手动填写", required = false) Long menuId) {
        return executeWithErrorHandling(() -> {
            SysMenuVo menu = new SysMenuVo();
            menu.setMenuType("C");
            if (menuId != null) {
                menu.setMenuId(menuId);
            } else {
                menu.setMenuName(menuName);
            }
            R<List<SysMenuVo>> menuResult = remoteMenuService.getMenuByParam(menu);

            if (menuResult.getCode() == 200) {
                List<SysMenuVo> list = menuResult.getData();
                if (list.isEmpty()) {
                    return AjaxResult.error("抱歉，未查询到对应菜单");
                }
                if (list.size() > 1) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("menuName", menuName);
                    return formSelect("查询出 " + list.size() + " 个对应菜单，请选择要打开的菜单：",
                        values, list, SysMenuVo::getMenuId, SysMenuVo::getMenuName, "menuId", "菜单");
                }
                return new AjaxResult(8001, "已为您打开" + list.getFirst().getMenuName() + "菜单。", list.getFirst().getComponent());
            }
            return AjaxResult.error("抱歉，未查询到对应菜单");
        }, "菜单导航");
    }
}