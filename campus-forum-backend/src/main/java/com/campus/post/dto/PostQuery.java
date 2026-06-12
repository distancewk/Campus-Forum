package com.campus.post.dto;

import com.campus.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQuery extends PageQuery {

    /** 板块筛选，可选 */
    private Long boardId;

    /** 排序方式：latest / hot / featured */
    private String sort = "latest";

    /** 搜索关键词 */
    private String keyword;
}
