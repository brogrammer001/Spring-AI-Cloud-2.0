package com.mall.system.api.factory;

import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteNoticeService;
import com.mall.system.api.domain.SysNotice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteNoticeFallbackFactory implements FallbackFactory<RemoteNoticeService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteNoticeFallbackFactory.class);

    @Override
    public RemoteNoticeService create(Throwable throwable)
    {
        log.error("通知公告服务调用失败:{}", throwable.getMessage());
        return new RemoteNoticeService()
        {
            @Override
            public R<List<SysNotice>> getNoticeList(SysNotice notice) {
                return R.fail("查询通知公告列表失败:" + throwable.getMessage());
            }

            @Override
            public R<SysNotice> getNoticeById(Long noticeId) {
                return R.fail("查询通知公告信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addNotice(SysNotice notice) {
                return R.fail("新增通知公告失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateNotice(SysNotice notice) {
                return R.fail("修改通知公告失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteNotice(Long noticeId) {
                return R.fail("删除通知公告失败:" + throwable.getMessage());
            }
        };
    }
}