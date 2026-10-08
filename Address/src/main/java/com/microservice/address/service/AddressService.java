package com.microservice.address.service;

import com.microservice.address.dto.AddressDto;
import com.microservice.address.entity.Address;
import com.microservice.address.repository.AddressRepository;
import com.microservice.address.utils.AppUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository repository;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${employee.service.url:http://EMPLOYEE}")
    private String employeeServiceUrl;

    public AddressService(AddressRepository repository) {
        this.repository = repository;
    }

    /**
     * VISUAL DEMONSTRATION OF REST TEMPLATE
     * Reaching out to the Employee Service to fetch a name!
     */
    public String getEmployeeNameForAddress(Long employeeId) {
        try {
            com.microservice.address.dto.EmployeeResponseDto response = restTemplate.getForObject(
                employeeServiceUrl + "/v1/Data/getEmployee/" + employeeId,
                com.microservice.address.dto.EmployeeResponseDto.class
            );
            return response != null ? response.getName() : "Unknown";
        } catch (Exception e) {
            return "Employee Service Down";
        }
    }

    public AddressDto saveAddress(AddressDto addressDto) {
        Address address = AppUtils.dtoToEntity(addressDto);
        Address savedAddress = repository.save(address);
        return AppUtils.entityToDto(savedAddress);
    }

    public List<AddressDto> getAllAddresses() {
        return repository.findAll().stream()
                .map(AppUtils::entityToDto)
                .collect(Collectors.toList());
    }

    public AddressDto getAddressById(Long id) {
        Address address = repository.findById(id).orElse(null);
        if (address != null) {
            return AppUtils.entityToDto(address);
        }
        return null;
    }

    public AddressDto updateAddress(Long id, AddressDto addressDto) {
        Address existingAddress = repository.findById(id).orElse(null);
        if (existingAddress != null) {
            existingAddress.setStreet(addressDto.getStreet());
            existingAddress.setCity(addressDto.getCity());
            existingAddress.setZipCode(addressDto.getZipCode());
            Address updatedAddress = repository.save(existingAddress);
            return AppUtils.entityToDto(updatedAddress);
        }
        return null;
    }

    public void deleteAddress(Long id) {
        repository.deleteById(id);
    }
}
