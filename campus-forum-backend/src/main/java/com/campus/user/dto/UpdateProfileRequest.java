package com.campus.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 20, message = "昵称长度为2-20个字符")
    private String nickname;

    @Size(max = 100, message = "个人简介最多100个字符")
    private String bio;
}
