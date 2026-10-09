package com.microservice.reactive;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class ReactiveController {

    // Mono promises to return EXACTLY 1 item (or 0) in the future.
    // Standard Spring would block the thread for 3 seconds. WebFlux releases the thread immediately!
    @GetMapping("/mono")
    public Mono<String> getSingleEmployee() {
        return Mono.just("Employee: Ashish")
                .delayElement(Duration.ofSeconds(3)); // Simulating a slow 3-second database call
    }

    // Flux promises to return MULTIPLE items (0 to N) in the future.
    // By using "text/event-stream", we are creating a Server-Sent Event (SSE). 
    // The browser will render the data piece-by-piece as it arrives!
    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getMultipleEmployeesStream() {
        return Flux.just("Ashish", "John", "Sarah", "Mike", "Emma")
                .delayElements(Duration.ofSeconds(1)) // Emits one name exactly every 1 second
                .map(name -> "Processing Employee: " + name + "\n");
    }
}
