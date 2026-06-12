package com.campus.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;

    /** 脱敏学号，如 2021****0001 */
    private String studentNo;

    private String nickname;

    private String avatar;

    private String bio;

    /** 发帖数 */
    private Integer postCount;

    /** 获赞数 */
    private Integer likeCount;

    private LocalDateTime createdAt;
}
