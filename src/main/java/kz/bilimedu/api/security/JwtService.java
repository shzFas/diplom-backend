package kz.bilimedu.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import kz.bilimedu.api.config.JwtProperties;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.springframework.stereotype.Service;

/**
 * Выпуск и разбор токенов.
 *
 * <p>Access-токен не хранится на сервере и живёт 15 минут. Refresh-токен —
 * случайные 32 байта; в базе лежит только его SHA-256, поэтому утечка таблицы
 * refresh_tokens не даёт войти под пользователем.
 */
@Service
public class JwtService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** Claims: sub — id пользователя, role, jti. */
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key)
                .compact();
    }

    /**
     * @throws ExpiredJwtException токен просрочен — контракт требует отдельный код TOKEN_EXPIRED
     * @throws JwtException        подпись не сходится или токен повреждён
     */
    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(Long.valueOf(claims.getSubject()), Role.valueOf(claims.get("role", String.class)));
    }

    /** Сырой refresh-токен, который увидит только клиент. */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** То, что попадает в базу вместо самого токена. */
    public String hashRefreshToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен в этой JVM", e);
        }
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plus(properties.refreshTtl());
    }
}
