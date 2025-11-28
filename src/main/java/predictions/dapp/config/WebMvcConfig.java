package predictions.dapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import predictions.dapp.audit.WebServiceAuditInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final WebServiceAuditInterceptor auditInterceptor;

    public WebMvcConfig(WebServiceAuditInterceptor auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/**");
    }
}