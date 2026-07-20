package com.mall.system.api.factory;

import java.util.List;
import com.mall.common.core.domain.R;
import com.mall.system.api.RemotePostService;
import com.mall.system.api.domain.SysPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemotePostFallbackFactory implements FallbackFactory<RemotePostService>
{
    private static final Logger log = LoggerFactory.getLogger(RemotePostFallbackFactory.class);

    @Override
    public RemotePostService create(Throwable throwable)
    {
        log.error("岗位服务调用失败:{}", throwable.getMessage());
        return new RemotePostService()
        {
            @Override
            public R<List<SysPost>> getPostList(SysPost post) {
                return R.fail("查询岗位列表失败:" + throwable.getMessage());
            }

            @Override
            public R<SysPost> getPostById(Long postId) {
                return R.fail("查询岗位信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addPost(SysPost post) {
                return R.fail("新增岗位失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updatePost(SysPost post) {
                return R.fail("修改岗位失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deletePost(Long postId) {
                return R.fail("删除岗位失败:" + throwable.getMessage());
            }
        };
    }
}