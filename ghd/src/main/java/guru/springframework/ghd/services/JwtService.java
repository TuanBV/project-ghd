package guru.springframework.ghd.services;

import guru.springframework.ghd.utils.CommonUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    public static final String TYPE_CLAIM = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.token.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpirationSeconds;

    public String generateAccessToken(String username) {
        return generateToken(username, TYPE_ACCESS, accessTokenExpirationSeconds);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, TYPE_REFRESH, refreshTokenExpirationSeconds);
    }

    private String generateToken(String username, String type, long expirationSeconds) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expirationSeconds * 1000);

        // jjwt 0.11: setClaims(Map) REPLACES the whole claims object, wiping out any
        // standard claim (jti/sub/iat/exp) set before it. Use claim(key, value) instead,
        // which merges into the existing claims - safe to combine with setId/setSubject/...
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, type)
                .setSubject(username)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public String extractJti(String token) {
        return getAllClaimsFromToken(token).getId();
    }

    public String extractType(String token) {
        return getAllClaimsFromToken(token).get(TYPE_CLAIM, String.class);
    }

    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            if (CommonUtil.isNotEmpty(token)) {
                return !isTokenExpired(token);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getUsernameFromJWT(String token) {
        return extractUsername(token);
    }

    /** Số giây còn lại tới khi token hết hạn - dùng làm TTL khi ghi vào Redis (blacklist/whitelist). */
    public long getRemainingSeconds(String token) {
        long remainingMillis = getExpirationDateFromToken(token).getTime() - System.currentTimeMillis();
        return Math.max(remainingMillis / 1000, 0);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

}
