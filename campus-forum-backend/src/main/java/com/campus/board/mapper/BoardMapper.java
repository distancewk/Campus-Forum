package com.campus.board.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.board.dto.BoardVO;
import com.campus.board.entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BoardMapper extends BaseMapper<Board> {

    /**
     * 查询板块列表（仅启用状态），联查帖子数，按 sortOrder 排序
     */
    List<BoardVO> selectBoardListWithPostCount();

    /**
     * 查询指定板块下的帖子数量（用于删除前检查）
     */
    Long selectPostCountByBoardId(Long boardId);

    /**
     * 逻辑删除板块（置 deleted=1），绝不物理删除。
     * 使用显式 @Update 以避开 MyBatis-Plus LambdaUpdateWrapper 在脱离 Spring 上下文时
     * 缺少实体 lambda 缓存的问题，保证单测可独立运行。
     */
    @Update("UPDATE board SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicalDeleteById(@Param("id") Long id);
}
