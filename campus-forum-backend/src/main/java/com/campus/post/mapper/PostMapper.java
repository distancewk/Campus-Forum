package com.campus.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.admin.dto.BoardStat;
import com.campus.post.dto.PostListVO;
import com.campus.post.dto.PostQuery;
import com.campus.post.dto.PostVO;
import com.campus.post.entity.Post;
import com.campus.search.dto.SearchQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 检查用户是否已点赞指定帖子
     */
    @Select("SELECT COUNT(*) FROM \"like\" WHERE user_id = #{userId} AND target_type = 'POST' AND target_id = #{postId}")
    int countLikeByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 检查用户是否已收藏指定帖子
     */
    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND post_id = #{postId}")
    int countFavoriteByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 分页查询帖子列表（联查 author / board），支持 boardId 筛选和三种排序
     */
    IPage<PostListVO> selectPostList(Page<PostListVO> page, @Param("query") PostQuery query);

    /**
     * 查询帖子详情（联查 author / board）
     */
    PostVO selectPostDetail(@Param("postId") Long postId);

    /**
     * 查询已发布帖子详情（联查 author / board）
     */
    PostVO selectPublishedPostDetail(@Param("postId") Long postId);

    /**
     * 浏览量 +1（原子操作）
     */
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 点赞数增减
     */
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 评论数增减
     */
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 收藏数增减
     */
    int updateFavCount(@Param("postId") Long postId, @Param("delta") int delta);

    /**
     * 批量重新计算近7天帖子的热度分
     */
    int recalculateHotScore();

    /**
     * 搜索帖子（标题或内容匹配关键词，可选板块筛选）
     */
    IPage<PostListVO> searchPosts(Page<PostListVO> page, @Param("query") SearchQuery query);

    /**
     * 查询总帖子数
     */
    @Select("SELECT COUNT(*) FROM post WHERE deleted = 0")
    Long selectTotalPosts();

    /**
     * 查询今日新增帖子数
     */
    @Select("SELECT COUNT(*) FROM post WHERE deleted = 0 AND created_at >= CURRENT_DATE")
    Long selectTodayNewPosts();

    /**
     * 查询待审核帖子列表，结果由 PostMapper.xml 定义
     */
    List<PostListVO> selectPendingPosts(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询待审核帖子总数
     */
    @Select("SELECT COUNT(*) FROM post WHERE status = 0 AND deleted = 0")
    long countPendingPosts();

    /**
     * 查询各板块帖子数统计，结果由 PostMapper.xml 定义
     */
    List<BoardStat> selectBoardStats();
}
