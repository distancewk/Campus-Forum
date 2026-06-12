package com.campus.board.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.board.dto.BoardCreateRequest;
import com.campus.board.dto.BoardUpdateRequest;
import com.campus.board.dto.BoardVO;
import com.campus.board.entity.Board;
import com.campus.board.mapper.BoardMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;

    /**
     * 获取启用状态的板块列表（按 sortOrder 排序），含帖子数
     */
    public List<BoardVO> listBoards() {
        return boardMapper.selectBoardListWithPostCount();
    }

    /**
     * 创建板块（校验名称唯一）
     */
    public BoardVO createBoard(BoardCreateRequest request) {
        // 校验名称唯一
        long count = boardMapper.selectCount(
                new LambdaQueryWrapper<Board>().eq(Board::getName, request.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "板块名称已存在");
        }

        Board board = new Board();
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setIcon(request.getIcon());
        board.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        board.setStatus(1);
        board.setCreatedAt(LocalDateTime.now());
        boardMapper.insert(board);

        // 返回创建后的 VO
        BoardVO vo = new BoardVO();
        vo.setId(board.getId());
        vo.setName(board.getName());
        vo.setDescription(board.getDescription());
        vo.setIcon(board.getIcon());
        vo.setPostCount(0);
        return vo;
    }

    /**
     * 更新板块
     */
    public void updateBoard(Long id, BoardUpdateRequest request) {
        Board board = boardMapper.selectById(id);
        if (board == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "板块不存在");
        }

        // 如果要改名，校验新名称唯一
        if (request.getName() != null && !request.getName().equals(board.getName())) {
            long count = boardMapper.selectCount(
                    new LambdaQueryWrapper<Board>().eq(Board::getName, request.getName()));
            if (count > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "板块名称已存在");
            }
            board.setName(request.getName());
        }

        if (request.getDescription() != null) {
            board.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            board.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            board.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            board.setStatus(request.getStatus());
        }

        boardMapper.updateById(board);
    }

    /**
     * 删除板块（需检查是否有帖子）
     */
    public void deleteBoard(Long id) {
        Board board = boardMapper.selectById(id);
        if (board == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "板块不存在");
        }

        // 检查板块下是否有帖子
        Long postCount = boardMapper.selectPostCountByBoardId(id);
        if (postCount != null && postCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该板块下仍有帖子，无法删除");
        }

        boardMapper.deleteById(id);
    }
}
