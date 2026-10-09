package com.microservice.Employee.services;

import com.microservice.Employee.dto.EmployeeDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    // KafkaTemplate is automatically configured by Spring Boot!
    private final KafkaTemplate<String, EmployeeDto> kafkaTemplate;
    
    // The exact topic we told Docker to create
    private static final String TOPIC = "employee-created-events";

    public KafkaProducerService(KafkaTemplate<String, EmployeeDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmployeeCreatedEvent(EmployeeDto employee) {
        System.out.println("🚀 [Kafka Producer] Publishing 'Employee Created' event for: " + employee.getName());
        // Fire and Forget!
        kafkaTemplate.send(TOPIC, employee);
    }
}
