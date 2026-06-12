package com.campus.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.interaction.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    /**
     * 批量查询用户已点赞的目标 ID 集合
     */
    @Select("<script>" +
            "SELECT target_id FROM \"like\" WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id IN " +
            "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    Set<Long> selectLikedTargetIds(@Param("userId") Long userId,
                                   @Param("targetType") String targetType,
                                   @Param("targetIds") List<Long> targetIds);
}
