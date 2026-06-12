package com.campus.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.interaction.entity.Favorite;
import com.campus.post.dto.PostListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 分页查询用户收藏的帖子（联查 post / author / board），按收藏时间倒序
     */
    List<PostListVO> selectFavoritePosts(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 查询用户收藏的帖子总数
     */
    long countFavoritePosts(@Param("userId") Long userId);
}
