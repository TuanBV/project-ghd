package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.auth.TokenInfoResponse;
import guru.springframework.ghd.utils.AES;
import guru.springframework.ghd.utils.CommonUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JwtService {
    public static final String PASSWORD = "refresh_token";

    @Value("${jwt.token.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expiration}")
    private String expirationTime;

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    public String generateToken(TokenInfoResponse tokenInfo) {

        Map<String, Object> claims = new HashMap<>();
        claims.put(PASSWORD, AES.encrypt(tokenInfo.getPassword(), aesSecretKey));

        Long expirationTimeLong = Long.parseLong(expirationTime);
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expirationTimeLong * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(tokenInfo.getUsername())
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

    public String extractPassword(String token) {
        return getAllClaimsFromToken(token).get(PASSWORD, String.class);
    }

    public String extractUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
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

    public String getPasswordFromJWT(String token) {
        return extractPassword(token);
    }



    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

}
