package com.campus.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("\"user\"")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String nickname;

    private String password;

    private String email;

    private String avatar;

    private String bio;

    /** STUDENT / ADMIN */
    private String role;

    /** 1=正常 0=禁用 */
    private Integer status;

    /** 连续登录失败次数 */
    private Integer loginFail;

    /** 锁定截止时间 */
    private LocalDateTime lockedUntil;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
