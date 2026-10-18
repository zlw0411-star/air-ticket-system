package com.airline.ticket.security;

import com.airline.ticket.user.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expireMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expire-hours}") long expireHours) {
        String value = secret == null ? "" : secret.strip();
        if (value.isEmpty()) {
            throw new IllegalStateException(
                    "缺少 app.jwt.secret / APP_JWT_SECRET：请通过环境变量提供本机私有密钥，仓库不提供密钥默认值。");
        }
        if (value.contains("change-me") || value.contains("please-change")) {
            throw new IllegalStateException(
                    "app.jwt.secret / APP_JWT_SECRET 仍在用例占位值，请替换为本机随机密钥。");
        }
        this.key = Keys.hmacShaKeyFor(pad(value).getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generate(SysUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private String pad(String secret) {
        if (secret.length() >= 32) {
            return secret;
        }
        return secret + "0".repeat(32 - secret.length());
    }
}
