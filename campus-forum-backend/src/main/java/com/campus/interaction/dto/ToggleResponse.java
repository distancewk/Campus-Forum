package com.campus.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToggleResponse {

    /** true=已点赞/收藏，false=已取消 */
    private Boolean active;

    /** 最新总数 */
    private Integer count;
}
