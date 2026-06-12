package com.campus.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAdminVO {

    private Long id;

    private String studentNo;

    private String nickname;

    private String email;

    /** STUDENT / ADMIN */
    private String role;

    /** 1=正常 0=禁用 */
    private Integer status;

    /** 发帖数量 */
    private Integer postCount;

    private LocalDateTime createdAt;

    private LocalDateTime lockedUntil;
}
