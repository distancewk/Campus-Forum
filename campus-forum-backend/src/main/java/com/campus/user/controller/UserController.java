package com.campus.user.controller;

import com.campus.common.response.R;
import com.campus.user.dto.UpdateProfileRequest;
import com.campus.user.dto.UserProfileVO;
import com.campus.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public R<UserProfileVO> getCurrentUser() {
        return R.ok(userService.getCurrentUser());
    }

    /**
     * 更新个人信息（昵称、简介）
     */
    @PutMapping("/me")
    public R<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        userService.updateProfile(request);
        return R.ok();
    }

    /**
     * 上传头像
     */
    @PostMapping("/me/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return R.ok(Map.of("avatarUrl", avatarUrl));
    }

    /**
     * 获取指定用户公开信息
     */
    @GetMapping("/{id}")
    public R<UserProfileVO> getUserById(@PathVariable Long id) {
        return R.ok(userService.getUserById(id));
    }
}
