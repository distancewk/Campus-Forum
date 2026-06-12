package com.campus.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 分页查询顶层评论（parent_id IS NULL，按时间正序）
     * 联查 author 信息
     */
    List<CommentVO> selectTopLevelComments(@Param("postId") Long postId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 查询顶层评论总数（用于分页）
     */
    long countTopLevelComments(@Param("postId") Long postId);

    /**
     * 批量查询一级回复（parent_id IN parentIds）
     * 联查 author 和 replyToUser 信息
     */
    List<CommentVO> selectRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 查询待审核评论列表
     */
    List<CommentVO> selectPendingComments(@Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 查询待审核评论总数
     */
    long countPendingComments();

    /**
     * 点赞数增减（原子操作）
     */
    int updateLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    /**
     * 查询已发布精华评论
     */
    Comment selectPublishedFeaturedComment(@Param("commentId") Long commentId);

    /**
     * 查询总评论数
     */
    @Select("SELECT COUNT(*) FROM comment WHERE deleted = 0")
    Long selectTotalComments();
}
