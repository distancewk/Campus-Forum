package com.campus.board.service;

import com.campus.board.entity.Board;
import com.campus.board.mapper.BoardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardMapper boardMapper;

    @Test
    void deleteBoardPerformsLogicDeleteNotPhysicalDelete() {
        Board board = new Board();
        board.setId(1L);
        when(boardMapper.selectById(1L)).thenReturn(board);
        when(boardMapper.selectPostCountByBoardId(1L)).thenReturn(0L);

        BoardService service = new BoardService(boardMapper);
        service.deleteBoard(1L);

        // 应执行逻辑删除（logicalDeleteById），而非物理删除（deleteById）
        verify(boardMapper).logicalDeleteById(1L);
        verify(boardMapper, never()).deleteById(any(Long.class));
    }
}
