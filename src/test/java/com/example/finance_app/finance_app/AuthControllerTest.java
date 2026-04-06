package com.example.finance_app.finance_app;

import com.example.finance_app.finance_app.config.CustomUserDetailsService;
import com.example.finance_app.finance_app.config.JwtUtil;
import com.example.finance_app.finance_app.config.SecurityConfig;
import com.example.finance_app.finance_app.controller.AuthController;
import com.example.finance_app.finance_app.entity.BlacklistedToken;
import com.example.finance_app.finance_app.entity.RefreshToken;
import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.exceptions.GlobalExceptionHandler;
import com.example.finance_app.finance_app.exceptions.TokenRefreshException;
import com.example.finance_app.finance_app.models.UserRole;
import com.example.finance_app.finance_app.models.dto.AuthDTO;
import com.example.finance_app.finance_app.repository.BlacklistedTokenRepository;
import com.example.finance_app.finance_app.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private final String username = "testuser";
    private final String password = "password123";
    private final String accessToken = "eyJhbGciOiJIUzUxMiJ9.xyz";
    private final String refreshTokenString = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username(username)
                .password("encodedPassword")
                .active(true)
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token(refreshTokenString)
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
    }

    @Test
    void login_Success() throws Exception {
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest(username, password);
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser.getId())).thenReturn(testRefreshToken);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshTokenString))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateAccessToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser.getId());
    }

    @Test
    void login_BadCredentials_ReturnsUnauthorized() throws Exception {
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest(username, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void login_ValidationError_ReturnsBadRequest() throws Exception {
        AuthDTO.LoginRequest invalidRequest = new AuthDTO.LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.password").exists());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void refreshToken_Success() throws Exception {
        AuthDTO.RefreshTokenRequest request = new AuthDTO.RefreshTokenRequest(refreshTokenString);

        when(refreshTokenService.verifyRefreshToken(refreshTokenString)).thenReturn(testRefreshToken);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn(accessToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshTokenString))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(refreshTokenService).verifyRefreshToken(refreshTokenString);
        verify(jwtUtil).generateAccessToken(testUser);
    }

    @Test
    void refreshToken_InvalidToken_ReturnsUnauthorized() throws Exception {
        AuthDTO.RefreshTokenRequest request = new AuthDTO.RefreshTokenRequest("invalid-token");

        when(refreshTokenService.verifyRefreshToken("invalid-token"))
                .thenThrow(new TokenRefreshException("Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void refreshToken_ExpiredToken_ReturnsUnauthorized() throws Exception {
        AuthDTO.RefreshTokenRequest request = new AuthDTO.RefreshTokenRequest(refreshTokenString);

        when(refreshTokenService.verifyRefreshToken(refreshTokenString))
                .thenThrow(new TokenRefreshException("Refresh token expired"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token expired"));
    }

    @Test
    void refreshToken_ValidationError_ReturnsBadRequest() throws Exception {
        AuthDTO.RefreshTokenRequest invalidRequest = new AuthDTO.RefreshTokenRequest("");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void logout_Success() throws Exception {
        AuthDTO.RefreshTokenRequest request = new AuthDTO.RefreshTokenRequest(refreshTokenString);

        doNothing().when(refreshTokenService).revokeRefreshToken(refreshTokenString);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(refreshTokenService).revokeRefreshToken(refreshTokenString);
    }

    @Test
    void logout_TokenNotFound_StillOk() throws Exception {
        AuthDTO.RefreshTokenRequest request = new AuthDTO.RefreshTokenRequest("non-existent-token");

        doNothing().when(refreshTokenService).revokeRefreshToken("non-existent-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(refreshTokenService).revokeRefreshToken("non-existent-token");
    }

    @Test
    void logout_ValidationError_ReturnsBadRequest() throws Exception {
        AuthDTO.RefreshTokenRequest invalidRequest = new AuthDTO.RefreshTokenRequest(null);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.refreshToken").exists());

        verify(refreshTokenService, never()).revokeRefreshToken(any());
    }
}