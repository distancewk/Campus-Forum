package com.campus.post.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.post.dto.*;
import com.campus.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 获取帖子列表（公开接口）
     */
    @GetMapping
    public R<PageResult<PostListVO>> listPosts(@Valid PostQuery query) {
        return R.ok(postService.listPosts(query));
    }

    /**
     * 获取帖子详情（公开接口）
     */
    @GetMapping("/{id}")
    public R<PostVO> getPostDetail(@PathVariable Long id) {
        return R.ok(postService.getPostDetail(id));
    }

    /**
     * 发布帖子（需认证）
     */
    @PostMapping
    public R<PostVO> createPost(@RequestBody @Valid PostCreateRequest request) {
        return R.ok(postService.createPost(request));
    }

    /**
     * 编辑帖子（需认证，仅作者）
     */
    @PutMapping("/{id}")
    public R<Void> updatePost(@PathVariable Long id,
                              @RequestBody @Valid PostUpdateRequest request) {
        postService.updatePost(id, request);
        return R.ok();
    }

    /**
     * 删除帖子（需认证，作者或管理员）
     */
    @DeleteMapping("/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return R.ok();
    }

    /**
     * 上传帖子图片（需认证）
     */
    @PostMapping("/upload-image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(postService.uploadImage(file));
    }
}
