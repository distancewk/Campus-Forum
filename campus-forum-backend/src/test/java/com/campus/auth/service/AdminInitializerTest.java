package com.campus.auth.service;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for V-C: AdminInitializer must NOT create/overwrite an admin
 * account when {@code campus.admin.password} is blank.
 */
@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer initializer(String adminPassword) {
        AdminInitializer initializer = new AdminInitializer(userMapper, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminStudentNo", "admin001");
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@school.edu.cn");
        ReflectionTestUtils.setField(initializer, "adminPassword", adminPassword);
        return initializer;
    }

    @Test
    void blankPasswordSkipsAdminCreation() {
        initializer("").run();

        verify(userMapper, never()).insert(any());
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void nullPasswordSkipsAdminCreation() {
        initializer(null).run();

        verify(userMapper, never()).insert(any());
    }

    @Test
    void configuredPasswordCreatesAdminWhenAbsent() {
        when(userMapper.selectByStudentNo("admin001")).thenReturn(null);
        when(passwordEncoder.encode("StrongPass#123")).thenReturn("ENC");

        initializer("StrongPass#123").run();

        verify(userMapper).insert(argThat(u ->
                "ADMIN".equals(u.getRole()) && "admin001".equals(u.getStudentNo())));
        verify(userMapper, never()).update(any(), any());
    }
}
