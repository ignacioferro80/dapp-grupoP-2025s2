package predictions.dapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // This enables the @Scheduled annotation in MethodCacheService
    // for automatic cleanup of expired cache entries
}