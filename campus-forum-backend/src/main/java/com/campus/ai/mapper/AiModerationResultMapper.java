package com.campus.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.ai.dto.AdminModerationVO;
import com.campus.ai.entity.AiModerationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiModerationResultMapper extends BaseMapper<AiModerationResult> {
    void insertResult(AiModerationResult result);

    int markReviewed(@Param("targetType") String targetType,
                     @Param("targetId") Long targetId,
                     @Param("status") String status,
                     @Param("adminId") Long adminId);

    List<AdminModerationVO> selectAdminModerationList(@Param("targetType") String targetType,
                                                      @Param("riskLevel") String riskLevel,
                                                      @Param("riskType") String riskType,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    long countAdminModerationList(@Param("targetType") String targetType,
                                  @Param("riskLevel") String riskLevel,
                                  @Param("riskType") String riskType);
}
