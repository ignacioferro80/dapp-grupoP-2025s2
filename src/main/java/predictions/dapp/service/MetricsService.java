package predictions.dapp.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

@Component
public class MetricsService {

    private final Counter predictionRequests;
    private final Counter predictionErrors;
    private final Timer predictionLatency;

    public MetricsService(MeterRegistry registry) {
        this.predictionRequests = Counter.builder("predictions_requests_total")
                .description("Cantidad de requests al endpoint de predicciones")
                .register(registry);

        this.predictionErrors = Counter.builder("predictions_errors_total")
                .description("Errores en el endpoint de predicciones")
                .tag("type", "internal")
                .register(registry);

        this.predictionLatency = Timer.builder("predictions_latency_seconds")
                .description("Latencia del endpoint de predicciones")
                .register(registry);
    }

    public void incrementRequests() {
        predictionRequests.increment();
    }

    public void incrementErrors() {
        predictionErrors.increment();
    }

    public <T> T measureLatency(java.util.concurrent.Callable<T> supplier) {
        try {
            return predictionLatency.recordCallable(supplier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
