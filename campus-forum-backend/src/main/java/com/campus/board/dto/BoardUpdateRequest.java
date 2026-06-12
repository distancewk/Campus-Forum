package com.campus.board.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BoardUpdateRequest {

    @Size(max = 50, message = "板块名称不能超过50个字符")
    private String name;

    @Size(max = 200, message = "板块描述不能超过200个字符")
    private String description;

    private String icon;

    private Integer sortOrder;

    /** 1=启用 0=禁用 */
    private Integer status;
}
