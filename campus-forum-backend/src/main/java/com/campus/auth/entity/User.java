package com.campus.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /**
     * 令牌版本号：每次登出 / 改密 / 封禁自增，使旧 token 立即失效。
     * insertStrategy/updateStrategy = NEVER：避免被其它入口的 INSERT/UPDATE 误写为 NULL 或重置为 0，
     * 列值由数据库 DEFAULT 0 初始化，自增通过 AuthService 的 setSql 完成。
     */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer tokenVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
