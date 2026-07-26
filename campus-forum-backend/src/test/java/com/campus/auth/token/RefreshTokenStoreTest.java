package com.campus.auth.token;

import com.campus.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the refresh-token store: jti issuance/validation/revocation are
 * persisted in Redis under {@code campus:rt:{jti}}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> vops;

    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(vops);
        store = new RefreshTokenStore(redis, "campus:rt:");
    }

    @Test
    void issueReturnsJtiAndStoresUnderCampusRtKey() {
        String jti = store.issue(1L, 600);

        assertThat(jti).isNotBlank();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(vops).set(keyCaptor.capture(), eq("1"), eq(600L), eq(TimeUnit.SECONDS));
        assertThat(keyCaptor.getValue()).contains("campus:rt:").endsWith(jti);
    }

    @Test
    void validateReturnsUserIdWhenPresent() {
        when(vops.get("campus:rt:rt:jti-1")).thenReturn("42");

        assertThat(store.validate("jti-1")).isEqualTo(42L);
    }

    @Test
    void validateThrowsWhenKeyAbsent() {
        when(vops.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> store.validate("missing"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void revokeDeletesKey() {
        store.revoke("jti-x");

        verify(redis).delete("campus:rt:rt:jti-x");
    }
}
