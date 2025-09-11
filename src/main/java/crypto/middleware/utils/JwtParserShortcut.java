package crypto.middleware.utils;

public class JwtParserShortcut {
    public static String roleFromToken(String token) {
        JwtUtil util = new JwtUtil(); // usa el mismo secreto/ttl por defecto
        return util.extractRole(token);
    }
}
