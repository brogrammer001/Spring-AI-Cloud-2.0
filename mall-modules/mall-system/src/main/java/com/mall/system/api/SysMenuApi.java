package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysMenu;
import com.mall.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class SysMenuApi extends BaseController {

    @Autowired
    private ISysMenuService menuService;

    @PostMapping("/getMenuByParam")
    @InnerAuth
    public R<List<SysMenu>> getMenuByParam(@RequestBody SysMenu menu) {
        Long userId = 1L;
        List<SysMenu> menus = menuService.selectMenuList(menu, userId);
        return R.ok(menus);
    }
}
