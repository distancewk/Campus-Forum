package com.campus.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者简要信息，帖子和评论中复用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorVO {

    private Long id;

    private String nickname;

    private String avatar;
}
