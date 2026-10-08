package com.microservice.Employee.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.microservice.Employee.dto.AddressResponseDto;

/**
 * Declarative REST Client to communicate with the Address Service.
 * 
 * If running locally with Eureka, it resolves "ADDRESS".
 * If running in Kubernetes, it reads the ADDRESS_SERVICE_URL from ConfigMap and uses K8s DNS!
 */
@FeignClient(name = "ADDRESS", url = "${address.service.url:}")
public interface AddressClient {

    @GetMapping("/v1/address/{id}")
    AddressResponseDto getAddressByEmployeeId(@PathVariable("id") Long id);

}
