package com.example.mcprice.service;

import com.example.mcprice.dto.LoginRequest;
import com.example.mcprice.dto.LoginResponse;
import com.example.mcprice.security.AppUserDetailsService;
import com.example.mcprice.security.AppUserPrincipal;
import com.example.mcprice.security.JwtService;
import com.example.mcprice.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return toResponse(principal);
    }

    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token khong hop le hoac da het han");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Token khong phai refresh token");
        }
        String username = jwtService.extractUsername(claims);
        AppUserPrincipal principal = (AppUserPrincipal) userDetailsService.loadUserByUsername(username);
        return toResponse(principal);
    }

    private LoginResponse toResponse(AppUserPrincipal principal) {
        String access = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);
        long expiresIn = appProperties.getJwt().getAccessTokenMinutes() * 60L;
        return new LoginResponse(access, refresh, principal.getUsername(), principal.getRole(), expiresIn);
    }
}
