package com.campus.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    /** 父评论 ID（楼中楼），可选 */
    private Long parentId;

    /** 回复目标用户 ID，可选 */
    private Long replyToUserId;
}
