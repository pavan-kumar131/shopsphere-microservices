package com.shopsphere.orders;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

/** Typed internal service-to-service client. Internal calls bypass the public gateway. */
@FeignClient(name = "product-service", url = "${shopsphere.product-service-url}")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    @PostMapping("/internal/products/{id}/reserve")
    void reserveStock(@PathVariable("id") Long id, @RequestHeader("X-Internal-Service-Token") String token, @RequestBody QuantityRequest request);

    @PostMapping("/internal/products/{id}/release")
    void releaseStock(@PathVariable("id") Long id, @RequestHeader("X-Internal-Service-Token") String token, @RequestBody QuantityRequest request);

    record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {}
    record QuantityRequest(Integer quantity) {}
}
