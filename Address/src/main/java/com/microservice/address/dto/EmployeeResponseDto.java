package com.microservice.address.dto;

/**
 * Data Transfer Object acting as a "catching mitt".
 * Used by RestTemplate to map JSON from the Employee service.
 */
public class EmployeeResponseDto {
    private String name;
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
