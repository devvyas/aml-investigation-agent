package com.aml.investigation.orchestrator;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Explicit executor for the CompletableFuture fan-out — deliberately not @Async anywhere on
 * this path. See Phase 6: @Async's proxy-mediated dispatch buys nothing once the orchestration
 * code is already managing an executor and building CompletableFutures by hand, and it can't
 * express per-future timeouts or distinguish "timed out" from "threw" the way this path needs.
 *
 * <p>Pool size is externalized configuration, not a hardcoded constant — Phase 6's sizing
 * principle is that the Claude API's shared rate limit is the real ceiling, not CPU, and that
 * each investigation fans out to three concurrent calls, so the actual limit values belong in
 * environment-specific config, not code.
 */
@Configuration
public class InvestigationExecutorConfig {

    @Bean
    public Executor investigationExecutor(
            @Value("${aml.orchestrator.executor.core-pool-size:4}") int corePoolSize,
            @Value("${aml.orchestrator.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${aml.orchestrator.executor.queue-capacity:50}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("investigation-");
        executor.initialize();
        return executor;
    }
}
