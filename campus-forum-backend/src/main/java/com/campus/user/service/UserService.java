package com.campus.user.service;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.util.FileUtil;
import com.campus.common.util.SecurityUtil;
import com.campus.user.dto.UpdateProfileRequest;
import com.campus.user.dto.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FileUtil fileUtil;

    /**
     * 获取当前登录用户信息（含发帖数、获赞数），学号脱敏
     */
    public UserProfileVO getCurrentUser() {
        Long userId = SecurityUtil.requireCurrentUserId();
        UserProfileVO vo = userMapper.selectProfileById(userId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        vo.setStudentNo(desensitizeStudentNo(vo.getStudentNo()));
        return vo;
    }

    /**
     * 获取指定用户公开信息（含发帖数、获赞数），学号脱敏
     */
    public UserProfileVO getUserById(Long id) {
        UserProfileVO vo = userMapper.selectProfileById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        vo.setStudentNo(desensitizeStudentNo(vo.getStudentNo()));
        return vo;
    }

    /**
     * 更新当前用户昵称和简介
     */
    public void updateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        User user = new User();
        user.setId(userId);
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (nickname.isEmpty() || nickname.length() > 20) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "昵称长度为1-20个字符");
            }
            user.setNickname(nickname);
        }
        if (request.getBio() != null) {
            String bio = request.getBio().trim();
            if (bio.length() > 200) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "简介不能超过200个字符");
            }
            user.setBio(bio);
        }
        userMapper.updateById(user);
    }

    /**
     * 上传头像，更新 user 表，返回头像可访问路径
     */
    public String uploadAvatar(MultipartFile file) {
        Long userId = SecurityUtil.requireCurrentUserId();
        String path = fileUtil.upload(file, "avatar/");
        userMapper.updateAvatar(userId, path);
        return path;
    }

    /**
     * 学号脱敏：保留前4位和后4位，中间用 **** 替换。
     * 例如 202100010001 -> 2021****0001
     */
    private String desensitizeStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() <= 8) {
            return studentNo;
        }
        return studentNo.substring(0, 4) + "****" + studentNo.substring(studentNo.length() - 4);
    }
}
