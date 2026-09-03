package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysConfig;
import com.mall.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class SysConfigApi extends BaseController {

    @Autowired
    private ISysConfigService configService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysConfig>> getConfigList(@RequestBody SysConfig config) {
        List<SysConfig> list = configService.selectConfigList(config);
        return R.ok(list);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addConfig(@RequestBody SysConfig config) {
        return R.ok(configService.insertConfig(config) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateConfig(@RequestBody SysConfig config) {
        return R.ok(configService.updateConfig(config) > 0);
    }

    @DeleteMapping("/{configId}")
    @InnerAuth
    public R<Boolean> deleteConfig(@PathVariable("configId") Long configId) {
        configService.deleteConfigByIds(new Long[]{configId});
        return R.ok(true);
    }

}