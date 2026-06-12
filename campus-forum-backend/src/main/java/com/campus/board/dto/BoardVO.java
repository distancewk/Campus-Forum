package com.campus.board.dto;

import lombok.Data;

@Data
public class BoardVO {

    private Long id;

    private String name;

    private String description;

    private String icon;

    /** 板块内帖子数 */
    private Integer postCount;
}
