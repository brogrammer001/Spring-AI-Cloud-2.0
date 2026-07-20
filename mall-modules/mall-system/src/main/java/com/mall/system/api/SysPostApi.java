package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysPost;
import com.mall.system.service.ISysPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/post")
public class SysPostApi extends BaseController {

    @Autowired
    private ISysPostService postService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysPost>> getPostList(@RequestBody SysPost post) {
        List<SysPost> posts = postService.selectPostList(post);
        return R.ok(posts);
    }

    @GetMapping("/{postId}")
    @InnerAuth
    public R<SysPost> getPostById(@PathVariable("postId") Long postId) {
        SysPost post = postService.selectPostById(postId);
        return R.ok(post);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addPost(@RequestBody SysPost post) {
        return R.ok(postService.insertPost(post) > 0);
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updatePost(@RequestBody SysPost post) {
        return R.ok(postService.updatePost(post) > 0);
    }

    @DeleteMapping("/{postId}")
    @InnerAuth
    public R<Boolean> deletePost(@PathVariable("postId") Long postId) {
        return R.ok(postService.deletePostById(postId) > 0);
    }

}