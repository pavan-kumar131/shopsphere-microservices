package com.shopsphere.payment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    public record CreatePayment(@NotNull Long orderId, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String currency) {}
    public record Payment(String paymentId, Long orderId, Long customerId, BigDecimal amount, String currency, String status, Instant createdAt) {}
    private final ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();
    private final boolean sandbox;
    public PaymentController(@Value("${shopsphere.payment.sandbox-only:true}") boolean sandbox) { this.sandbox = sandbox; }

    @PostMapping
    public Payment create(@Valid @RequestBody CreatePayment request,
                          @RequestHeader(value = "Idempotency-Key", required = false) String key,
                          HttpServletRequest httpRequest) {
        if (!sandbox) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure a real payment provider adapter before enabling live payments");
        if (key == null || key.isBlank() || key.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key is required (maximum 100 characters)");
        Long userId = userId(httpRequest);
        String id = "sandbox_" + UUID.nameUUIDFromBytes((userId + ":" + request.orderId() + ":" + key).getBytes()).toString();
        return payments.computeIfAbsent(id, ignored -> new Payment(id, request.orderId(), userId, request.amount(), request.currency().toUpperCase(), "REQUIRES_TEST_CONFIRMATION", Instant.now()));
    }

    @PostMapping("/{id}/confirm-sandbox")
    public Payment confirm(@PathVariable String id, @RequestParam(defaultValue = "success") String outcome) {
        if (!sandbox) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sandbox disabled");
        Payment old = payments.get(id);
        if (old == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        if (!old.status().equals("REQUIRES_TEST_CONFIRMATION")) return old;
        Payment updated = new Payment(old.paymentId(), old.orderId(), old.customerId(), old.amount(), old.currency(), "success".equalsIgnoreCase(outcome) ? "SUCCEEDED" : "FAILED", old.createdAt());
        payments.put(id, updated);
        return updated;
    }

    @GetMapping("/{id}")
    public Payment get(@PathVariable String id, HttpServletRequest request) {
        Payment payment = payments.get(id);
        if (payment == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        if (!"ADMIN".equals(request.getAttribute("shopsphere.role")) && !payment.customerId().equals(userId(request))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment belongs to another customer");
        return payment;
    }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("shopsphere.userId");
        if (value instanceof Long id) return id;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid bearer token required");
    }
}
