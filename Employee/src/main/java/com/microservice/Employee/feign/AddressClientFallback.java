package com.microservice.Employee.feign;

import com.microservice.Employee.dto.AddressResponseDto;
import org.springframework.stereotype.Component;

/**
 * Feign Fallback Class
 * If the AddressClient fails or the Circuit Breaker trips to OPEN,
 * Spring automatically routes the call to this class instead.
 */
@Component
public class AddressClientFallback implements AddressClient {

    @Override
    public AddressResponseDto getAddressByEmployeeId(Long id) {
        System.out.println("⚠️ Feign Circuit Breaker Triggered! Address Service is unavailable. Returning fallback data.");
        
        // Provide safe, default data without making any network calls
        AddressResponseDto fallbackAddress = new AddressResponseDto();
        fallbackAddress.setStreet("Service Unavailable");
        fallbackAddress.setCity("Service Unavailable");
        fallbackAddress.setZipCode("000000");
        
        return fallbackAddress;
    }
}
