package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysNotice;
import com.mall.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class SysNoticeApi extends BaseController {

    @Autowired
    private ISysNoticeService noticeService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysNotice>> getNoticeList(@RequestBody SysNotice notice) {
        List<SysNotice> notices = noticeService.selectNoticeList(notice);
        return R.ok(notices);
    }

    @GetMapping("/{noticeId}")
    @InnerAuth
    public R<SysNotice> getNoticeById(@PathVariable("noticeId") Long noticeId) {
        SysNotice notice = noticeService.selectNoticeById(noticeId);
        return R.ok(notice);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addNotice(@RequestBody SysNotice notice) {
        return R.ok(noticeService.insertNotice(notice) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateNotice(@RequestBody SysNotice notice) {
        return R.ok(noticeService.updateNotice(notice) > 0);
    }

    @DeleteMapping("/{noticeId}")
    @InnerAuth
    public R<Boolean> deleteNotice(@PathVariable("noticeId") Long noticeId) {
        return R.ok(noticeService.deleteNoticeById(noticeId) > 0);
    }

}