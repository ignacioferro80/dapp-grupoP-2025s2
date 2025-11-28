package predictions.dapp.cache;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution capability
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // This enables @Scheduled annotations to work
}