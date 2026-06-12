package com.campus.ai.dto;

import com.campus.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminModerationQuery extends PageQuery {
    private String targetType;
    private String riskLevel;
    private String riskType;
}
