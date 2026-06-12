package com.campus.board.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.board.dto.BoardVO;
import com.campus.board.entity.Board;
import org.apache.ibatis.annotations.Mapper;

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
}
