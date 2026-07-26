package com.campus.board.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.board.entity.Board;
import com.campus.board.mapper.BoardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

        // 应执行逻辑删除（update），而非物理删除（deleteById）
        ArgumentCaptor<LambdaUpdateWrapper<Board>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(boardMapper).update(isNull(), captor.capture());
        verify(boardMapper, never()).deleteById(any(Long.class));

        // SET 子句应包含 deleted，与 @TableLogic 逻辑删除一致
        assertThat(captor.getValue().getSqlSet()).contains("deleted");
    }
}
