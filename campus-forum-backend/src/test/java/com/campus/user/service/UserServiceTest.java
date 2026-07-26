package com.campus.user.service;

import com.campus.auth.mapper.UserMapper;
import com.campus.common.util.FileUtil;
import com.campus.user.dto.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private FileUtil fileUtil;

    @Test
    void getUserByIdDesensitizesStudentNo() {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(1L);
        vo.setStudentNo("202100010001");
        vo.setNickname("测试用户");
        when(userMapper.selectProfileById(1L)).thenReturn(vo);

        UserService service = new UserService(userMapper, fileUtil);
        UserProfileVO result = service.getUserById(1L);

        assertThat(result.getStudentNo()).isEqualTo("2021****0001");
    }
}
