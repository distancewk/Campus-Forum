package com.campus.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LikeRequest {

    @NotBlank(message = "目标类型不能为空")
    @Pattern(regexp = "^(POST|COMMENT)$", message = "目标类型只能是 POST 或 COMMENT")
    private String targetType;

    @NotNull(message = "目标 ID 不能为空")
    private Long targetId;
}
