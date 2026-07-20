package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysNotice;
import com.mall.system.api.factory.RemoteNoticeFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remoteNoticeService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteNoticeFallbackFactory.class)
public interface RemoteNoticeService {

    @PostMapping("/api/notice/list")
    public R<List<SysNotice>> getNoticeList(@RequestBody SysNotice notice);

    @GetMapping("/api/notice/{noticeId}")
    public R<SysNotice> getNoticeById(@PathVariable("noticeId") Long noticeId);

    @PostMapping("/api/notice/add")
    public R<Boolean> addNotice(@RequestBody SysNotice notice);

    @PutMapping("/api/notice/update")
    public R<Boolean> updateNotice(@RequestBody SysNotice notice);

    @DeleteMapping("/api/notice/{noticeId}")
    public R<Boolean> deleteNotice(@PathVariable("noticeId") Long noticeId);

}