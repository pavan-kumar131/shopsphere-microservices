package com.shopsphere.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/products")
public class InternalInventoryController {
    public record QuantityRequest(@NotNull @Min(1) Integer quantity) {}
    private final ProductRepository products;
    private final InternalServiceVerifier verifier;
    public InternalInventoryController(ProductRepository products, InternalServiceVerifier verifier) { this.products = products; this.verifier = verifier; }

    @PostMapping("/{id}/reserve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void reserve(@RequestHeader("X-Internal-Service-Token") String token, @PathVariable Long id, @RequestBody QuantityRequest request) {
        verifier.require(token);
        if (request == null || request.quantity() == null || request.quantity() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
        if (products.reserveStock(id, request.quantity()) == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Product missing or insufficient stock");
    }

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void release(@RequestHeader("X-Internal-Service-Token") String token, @PathVariable Long id, @RequestBody QuantityRequest request) {
        verifier.require(token);
        if (request == null || request.quantity() == null || request.quantity() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
        if (products.releaseStock(id, request.quantity()) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    }
}
