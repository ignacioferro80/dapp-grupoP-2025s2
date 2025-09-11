package crypto.middleware.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // ► Secreto y TTL embebidos (no usa application.properties)
    //   SECRET: 256-bit (HS256). Puedes regenerarlo si quieres.
    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor("A1u7h0r1z3-12345678-DEV-256bits-Secret-Key-ChangeMe!".getBytes());
    private static final long TTL_MILLIS = 24 * 60 * 60 * 1000L; // 24h

    public String generateToken(String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TTL_MILLIS))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return getAllClaims(token).get("role", String.class);
    }

    public boolean validate(String token, String expectedEmail) {
        final String email = extractUsername(token);
        return email != null && email.equalsIgnoreCase(expectedEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date exp = getClaim(token, Claims::getExpiration);
        return exp.before(new Date());
    }

    private <T> T getClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(getAllClaims(token));
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
