package com.microservice.Employee.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    /**
     * Runs on a background thread.
     * Protected by Resilience4j TimeLimiter to prevent Thread Starvation.
     */
    @Async("asyncExecutor")
    @TimeLimiter(name = "emailService", fallbackMethod = "emailTimeoutFallback")
    public CompletableFuture<Void> sendWelcomeEmail(String employeeName) {
        System.out.println("⏳ [Background] Starting email for " + employeeName + " on thread: " + Thread.currentThread().getName());
        
        try {
            // Simulate a broken email server that gets stuck for 10 seconds!
            Thread.sleep(10000); 
        } catch (InterruptedException e) {
            // When TimeLimiter reaches 2 seconds, it FORCEFULLY kills this sleep to free the thread!
            System.out.println("🛑 [Background] Thread FORCEFULLY KILLED by TimeLimiter to prevent starvation!");
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(null);
        }
        
        System.out.println("✅ [Background] Welcome Email successfully sent to: " + employeeName + "!");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * This automatically runs if the main thread takes longer than 2 seconds.
     */
    public CompletableFuture<Void> emailTimeoutFallback(String employeeName, Exception e) {
        System.out.println("⚠️ [Fallback] Email to " + employeeName + " timed out. Dropping task to save RAM.");
        return CompletableFuture.completedFuture(null);
    }
}
