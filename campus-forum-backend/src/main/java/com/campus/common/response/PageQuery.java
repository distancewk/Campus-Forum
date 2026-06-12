package com.campus.common.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(50)
    private int size = 20;

    public int getOffset() {
        return (page - 1) * size;
    }
}
