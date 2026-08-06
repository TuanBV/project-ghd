package com.example.mcprice.security;

import com.example.mcprice.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final AppProperties appProperties;

    private Key signingKey() {
        String secret = appProperties.getJwt().getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(AppUserPrincipal principal) {
        return buildToken(principal, TYPE_ACCESS, appProperties.getJwt().getAccessTokenMinutes(), ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(AppUserPrincipal principal) {
        return buildToken(principal, TYPE_REFRESH, appProperties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS);
    }

    private String buildToken(AppUserPrincipal principal, String type, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claims(Map.of(CLAIM_ROLE, principal.getRole(), CLAIM_TYPE, type, "uid", principal.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }
}
