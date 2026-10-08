package com.microservice.address.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.microservice.address.dto.AddressDto;
import com.microservice.address.service.AddressService;

@RestController
@RequestMapping("/v1/address")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @PostMapping
    @CacheEvict(value = "addresses", allEntries = true)
    public ResponseEntity<AddressDto> createAddress(@RequestBody AddressDto addressDto) {
        AddressDto savedAddress = service.saveAddress(addressDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAddress);
    }

    @GetMapping
    @Cacheable(value = "addresses")
    public ResponseEntity<List<AddressDto>> getAllAddresses() {
        return ResponseEntity.ok(service.getAllAddresses());
    }

    // VISUAL DEMONSTRATION ENDPOINT
    @GetMapping("/employee-name/{id}")
    public ResponseEntity<String> getEmployeeName(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEmployeeNameForAddress(id));
    }

    @GetMapping("/{id}")
    @Cacheable(value = "addresses", key = "#id")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable Long id) {
        AddressDto address = service.getAddressById(id);
        if (address != null) {
            return ResponseEntity.ok(address);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "addresses", allEntries = true)
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @RequestBody AddressDto addressDto) {
        AddressDto updatedAddress = service.updateAddress(id, addressDto);
        if (updatedAddress != null) {
            return ResponseEntity.ok(updatedAddress);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "addresses", allEntries = true)
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        AddressDto address = service.getAddressById(id);
        if (address != null) {
            service.deleteAddress(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
