package com.microservice.Employee.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.microservice.Employee.dto.EmployeeDto;
import com.microservice.Employee.dto.EmployeeWithAddressDto;
import com.microservice.Employee.services.EmployeeService;

/**
 * REST Controller for Employee API endpoints.
 * It handles incoming HTTP requests (like GET, POST) and delegates business logic to the EmployeeService.
 */
@RestController
@RequestMapping("/v1/Data")
@Tag(name = "Employee API", description = "Endpoints for managing Employees in the WorkSphere Application")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping("/employee")
    public String getEmployee() {
        return "Employee";
    }

    // --- V1 Flow (In-Memory @Async) ---
    @Operation(summary = "Save Employee (Async V1)", description = "Saves an employee and sends a welcome email in the background using @Async.")
    @PostMapping("/saveEmployee")
    public ResponseEntity<EmployeeDto> saveEmployee(@RequestBody EmployeeDto dto){
        EmployeeDto employee = service.saveEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    // --- V2 Flow (Kafka Event-Driven) ---
    @Operation(summary = "Save Employee (Kafka V2)", description = "Saves an employee and publishes a message to Kafka for asynchronous email processing.")
    @PostMapping("/v2/saveEmployee")
    public ResponseEntity<EmployeeDto> saveEmployeeV2(@RequestBody EmployeeDto dto){
        EmployeeDto employee = service.saveEmployeeV2(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    @Operation(summary = "Get All Employees", description = "Fetches a list of all employees in the database.")
    @GetMapping("/getAllEmployee")
    public ResponseEntity<List<EmployeeDto>> getAllEmployee(){
        List<EmployeeDto> allEmployee = service.getAllEmployee();
        return ResponseEntity.ok(allEmployee);
    }

    @GetMapping("/getEmployee/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id){
        EmployeeDto employee = service.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/updateEmployee/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDto dto){
        EmployeeDto employee = service.updateEmployee(id, dto);
        return ResponseEntity.ok(employee);
    }

    @DeleteMapping("/deleteEmployee/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id){
        service.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get Employee with Address (Circuit Breaker)", description = "Fetches employee details and makes a Feign Client call to the Address Service. Protected by Resilience4j.")
    @GetMapping("/getEmployeeWithAddress/{id}")
    public ResponseEntity<EmployeeWithAddressDto> getEmployeeWithAddress(@PathVariable Long id){
        return ResponseEntity.ok(service.getEmployeeWithAddress(id));
    }
}
