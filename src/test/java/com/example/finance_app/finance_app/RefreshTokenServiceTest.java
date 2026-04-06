package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.entity.RefreshToken;
import com.example.finance_app.finance_app.entity.RefreshTokenRepository;
import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.exceptions.TokenRefreshException;
import com.example.finance_app.finance_app.repository.UserRepository;
import com.example.finance_app.finance_app.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private final Long userId = 1L;
    private final String tokenString = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .password("password")
                .active(true)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token(tokenString)
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);
    }

    @Test
    void createRefreshToken_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(2L);
            return token;
        });

        RefreshToken created = refreshTokenService.createRefreshToken(userId);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals(testUser, created.getUser());
        assertNotNull(created.getToken());
        assertFalse(created.isRevoked());
        assertTrue(created.getExpiryDate().isAfter(Instant.now()));

        verify(userRepository).findById(userId);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_UserNotFound_ThrowsRuntimeException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.createRefreshToken(userId));
        assertEquals("User not found", exception.getMessage());

        verify(refreshTokenRepository, never()).deleteByUser(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyRefreshToken_Success() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.of(testRefreshToken));

        RefreshToken verified = refreshTokenService.verifyRefreshToken(tokenString);

        assertEquals(testRefreshToken, verified);
        verify(refreshTokenRepository).findByTokenAndRevokedFalse(tokenString);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyRefreshToken_TokenNotFound_ThrowsTokenRefreshException() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.empty());

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
                () -> refreshTokenService.verifyRefreshToken(tokenString));
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyRefreshToken_TokenExpired_ThrowsTokenRefreshExceptionAndRevokes() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(tokenString)
                .user(testUser)
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expiredToken);

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
                () -> refreshTokenService.verifyRefreshToken(tokenString));
        assertEquals("Refresh token expired", exception.getMessage());

        assertTrue(expiredToken.isRevoked());
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    void revokeRefreshToken_TokenExists_RevokesAndSaves() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        refreshTokenService.revokeRefreshToken(tokenString);

        assertTrue(testRefreshToken.isRevoked());
        verify(refreshTokenRepository).findByTokenAndRevokedFalse(tokenString);
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    void revokeRefreshToken_TokenNotFound_DoesNothing() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken(tokenString);

        verify(refreshTokenRepository).findByTokenAndRevokedFalse(tokenString);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeRefreshToken_TokenAlreadyRevoked_DoesNothing() {
        testRefreshToken.setRevoked(true);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenString))
                .thenReturn(Optional.empty()); // revoked tokens are not found by the query

        refreshTokenService.revokeRefreshToken(tokenString);

        verify(refreshTokenRepository).findByTokenAndRevokedFalse(tokenString);
        verify(refreshTokenRepository, never()).save(any());
    }
}