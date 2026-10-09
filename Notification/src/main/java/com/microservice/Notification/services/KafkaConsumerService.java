package com.microservice.Notification.services;

import com.microservice.Notification.dto.EmployeeDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    // This annotation tells Spring to continuously listen to this Kafka topic
    @KafkaListener(topics = "employee-created-events", groupId = "notification-group")
    public void consumeEmployeeEvent(EmployeeDto employee) {
        System.out.println("==========================================");
        System.out.println("📥 [KAFKA EVENT RECEIVED]");
        System.out.println("👤 New Employee Detected: " + employee.getName());
        System.out.println("📧 Sending Welcome Email to: " + employee.getEmail());
        
        try {
            // Simulate sending a real email
            Thread.sleep(3000); 
            System.out.println("✅ Email Successfully Sent!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("==========================================");
    }
}
