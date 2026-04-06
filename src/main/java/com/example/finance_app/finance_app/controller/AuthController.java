package com.example.finance_app.finance_app.controller;

import com.example.finance_app.finance_app.config.JwtUtil;
import com.example.finance_app.finance_app.entity.BlacklistedToken;
import com.example.finance_app.finance_app.entity.RefreshToken;
import com.example.finance_app.finance_app.entity.User;
import com.example.finance_app.finance_app.models.dto.AuthDTO;
import com.example.finance_app.finance_app.repository.BlacklistedTokenRepository;
import com.example.finance_app.finance_app.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private final BlacklistedTokenRepository blacklistedTokenRepository;
//    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO.LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal(); // ✅ SAFE

        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(
                new AuthDTO.JwtResponse(
                        accessToken,
                        refreshTokenEntity.getToken(),
                        "Bearer"
                )
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody AuthDTO.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        return ResponseEntity.ok(new AuthDTO.JwtResponse(newAccessToken, request.getRefreshToken(), "Bearer"));
    }

    /*@PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody AuthDTO.RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }*/

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @Valid @RequestBody AuthDTO.RefreshTokenRequest refreshRequest) {
        // 1. Revoke the refresh token
        refreshTokenService.revokeRefreshToken(refreshRequest.getRefreshToken());

        // 2. Blacklist the current access token (from Authorization header)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                Date expiration = jwtUtil.extractExpiration(accessToken);
                if (expiration != null && expiration.after(new Date())) {
                    BlacklistedToken blacklisted = BlacklistedToken.builder()
                            .token(accessToken)
                            .expiryDate(expiration.toInstant())
                            .build();
                    blacklistedTokenRepository.save(blacklisted);
                }
            } catch (Exception e) {
                // If token is malformed or already expired, we can ignore
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }
        log.info("Logged out successfully");
        return ResponseEntity.ok("Logged out successfully");
    }
}