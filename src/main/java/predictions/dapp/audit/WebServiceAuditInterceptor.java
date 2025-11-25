package predictions.dapp.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import predictions.dapp.security.JwtUtil;

import java.time.Instant;
import java.util.Arrays;

@Component
public class WebServiceAuditInterceptor implements HandlerInterceptor {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    private final JwtUtil jwtUtil;

    public WebServiceAuditInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        long start = System.currentTimeMillis();
        request.setAttribute("auditStartTime", start);

        auditLogger.info("➡️  Incoming request: method={}, endpoint={}, params={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getParameterMap()
        );

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        long start = (long) request.getAttribute("auditStartTime");
        long executionTime = System.currentTimeMillis() - start;

        String token = JwtUtil.extractTokenFromRequest(request);
        String user = "anonymous";

        if (token != null) {
            try {
                user = jwtUtil.extractUsername(token);
            } catch (Exception ignored) {
                // El token puede ser inválido → user permanece como "anonymous"
            }
        }

        String method = request.getMethod();
        String endpoint = request.getRequestURI();
        String params = request.getParameterMap().isEmpty() ?
                "none" :
                Arrays.toString(request.getParameterMap().entrySet().toArray());

        auditLogger.info(
                "📝 AUDIT | ts={} | user={} | method={} | endpoint={} | params={} | execTime={}ms | status={}",
                Instant.now(),
                user,
                method,
                endpoint,
                params,
                executionTime,
                response.getStatus()
        );

        if (ex != null) {
            auditLogger.error("❌ Exception thrown during request: {}", ex.getMessage());
        }
    }
}
