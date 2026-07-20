package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysPost;
import com.mall.system.api.factory.RemotePostFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remotePostService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemotePostFallbackFactory.class)
public interface RemotePostService {

    @PostMapping("/api/post/list")
    public R<List<SysPost>> getPostList(@RequestBody SysPost post);

    @GetMapping("/api/post/{postId}")
    public R<SysPost> getPostById(@PathVariable("postId") Long postId);

    @PostMapping("/api/post/add")
    public R<Boolean> addPost(@RequestBody SysPost post);

    @PutMapping("/api/post/update")
    public R<Boolean> updatePost(@RequestBody SysPost post);

    @DeleteMapping("/api/post/{postId}")
    public R<Boolean> deletePost(@PathVariable("postId") Long postId);

}