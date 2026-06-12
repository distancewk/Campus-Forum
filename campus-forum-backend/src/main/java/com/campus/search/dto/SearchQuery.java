package com.campus.search.dto;

import com.campus.common.response.PageQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchQuery extends PageQuery {

    /** 搜索关键词（必填） */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 板块内搜索，可选 */
    private Long boardId;
}
