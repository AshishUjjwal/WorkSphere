package com.microservice.Employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Asynchronous Background Tasks.
 * We must explicitly define a ThreadPoolTaskExecutor. If we don't, Spring Boot
 * will create a new thread for every single task (SimpleAsyncTaskExecutor).
 * Under heavy load, this will consume all RAM and cause a Kubernetes OOMKilled crash!
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Keep 2 background threads alive at minimum
        executor.setMaxPoolSize(5);       // Max 5 threads to prevent memory crashes
        executor.setQueueCapacity(50);    // Queue up to 50 tasks if all 5 threads are busy
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}
