package com.campus.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteRequest {

    @NotNull(message = "帖子 ID 不能为空")
    private Long postId;
}
