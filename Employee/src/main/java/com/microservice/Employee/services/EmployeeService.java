package com.microservice.Employee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microservice.Employee.dto.EmployeeDto;
import com.microservice.Employee.entity.Employee;
import com.microservice.Employee.repository.EmployeeRepository;
import com.microservice.Employee.utils.AppUtils;
import com.microservice.Employee.exception.ResourceNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.microservice.Employee.feign.AddressClient;
import com.microservice.Employee.dto.AddressResponseDto;
import com.microservice.Employee.dto.EmployeeWithAddressDto;

import org.springframework.transaction.annotation.Transactional;

/**
 * Service class containing business logic for Employee operations.
 * It acts as an intermediary between the EmployeeController and EmployeeRepository.
 */
@Service 
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository repository;
    
    @Autowired
    private AddressClient addressClient;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;

    // --- V1 Flow: Uses in-memory @Async ---
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDto saveEmployee(EmployeeDto dto) {
        Employee employeeEntity = AppUtils.dtoToEntity(dto);
        Employee savedEntity = repository.save(employeeEntity); 
        
        notificationService.sendWelcomeEmail(savedEntity.getName());
        return AppUtils.entityToDto(savedEntity); 
    }

    // --- V2 Flow: Uses Apache Kafka (Event-Driven) ---
    // @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDto saveEmployeeV2(EmployeeDto dto) {
        Employee employeeEntity = AppUtils.dtoToEntity(dto);
        Employee savedEntity = repository.save(employeeEntity); 
        EmployeeDto savedDto = AppUtils.entityToDto(savedEntity);
        
        // Broadcast the event to Kafka! We don't care who is listening.
        kafkaProducerService.sendEmployeeCreatedEvent(savedDto);
        
        return savedDto;
    }

    @Cacheable(value = "employees")
    public List<EmployeeDto> getAllEmployee() {
        List<Employee> allEmployee = repository.findAll();
        return allEmployee.stream().map(AppUtils::entityToDto).collect(Collectors.toList());
    }

    @Cacheable(value = "employees", key = "#id")
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return AppUtils.entityToDto(employee);
    }

    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee existingEmployee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        existingEmployee.setName(dto.getName());
        existingEmployee.setEmail(dto.getEmail());
        existingEmployee.setPhone(dto.getPhone());
        existingEmployee.setAddress(dto.getAddress());
        
        Employee updatedEmployee = repository.save(existingEmployee);
        return AppUtils.entityToDto(updatedEmployee);
    }

    @CacheEvict(value = "employees", allEntries = true)
    public void deleteEmployee(Long id) {
        Employee existingEmployee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        repository.delete(existingEmployee);
    }

    public EmployeeWithAddressDto getEmployeeWithAddress(Long id) {
        // 1. Fetch Employee from our Database
        EmployeeDto employee = getEmployeeById(id);
        
        // 2. Call Address Microservice using our new Feign Client!
        // The Fallback logic is now completely hidden inside AddressClientFallback.java!
        AddressResponseDto addressResponse = addressClient.getAddressByEmployeeId(id);
        
        return new EmployeeWithAddressDto(employee, addressResponse);
    }
}
