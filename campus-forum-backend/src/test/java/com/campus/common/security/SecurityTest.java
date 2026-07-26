package com.campus.common.security;

import com.campus.auth.mapper.UserMapper;
import com.campus.common.config.SecurityConfig;
import com.campus.common.ratelimit.RateLimitInterceptor;
import com.campus.common.util.JwtUtil;
import com.campus.search.controller.SearchController;
import com.campus.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // JwtAuthenticationFilter 是真实 Bean（保证请求能继续经过安全链），但其两个非 Redis 依赖用 Mock 注入。
    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtUtil jwtUtil;

    // RateLimitInterceptor 在 Web 切片中需要，但依赖 Redis；Mock 掉使其无需外部 Redis 即可加载上下文。
    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @Test
    void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
