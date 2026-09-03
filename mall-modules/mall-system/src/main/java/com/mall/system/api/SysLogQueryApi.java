package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysLogininfor;
import com.mall.system.api.domain.SysOperLog;
import com.mall.system.service.ISysLogininforService;
import com.mall.system.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SysLogQueryApi extends BaseController {

    @Autowired
    private ISysOperLogService operLogService;

    @Autowired
    private ISysLogininforService logininforService;

    @PostMapping("/operlog/list")
    @InnerAuth
    public R<List<SysOperLog>> getOperLogList(@RequestBody SysOperLog operLog) {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return R.ok(list);
    }

    @PostMapping("/logininfor/list")
    @InnerAuth
    public R<List<SysLogininfor>> getLogininforList(@RequestBody SysLogininfor logininfor) {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        return R.ok(list);
    }

}