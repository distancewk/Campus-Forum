package com.campus.post.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostUpdateRequest {

    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    private String content;
}
