package com.campus.admin.dto;

import com.campus.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQuery extends PageQuery {

    /** 搜索关键词（昵称或学号） */
    private String keyword;

    /** 用户状态筛选（0=禁用 1=正常） */
    private Integer status;
}
