package com.shopsphere.orders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    public record Line(@NotNull Long productId, @Min(1) int quantity) {}
    public record Checkout(@NotEmpty List<@Valid Line> items) {}
    private final OrderRepository orders;
    private final TokenClaims tokens;
    private final ProductClient products;
    private final String internalToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderController(OrderRepository orders, TokenClaims tokens, ProductClient products,
                           @Value("${shopsphere.internal-token}") String internalToken) {
        this.orders = orders; this.tokens = tokens; this.products = products; this.internalToken = internalToken;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderRecord checkout(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotency,
                                @Valid @RequestBody Checkout request) {
        TokenClaims.Claims claims = tokens.require(auth);
        if (!"CUSTOMER".equals(claims.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer role required");
        if (idempotency == null || idempotency.isBlank() || idempotency.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header required (maximum 100 characters)");
        var prior = orders.findByCustomerIdAndIdempotencyKey(claims.userId(), idempotency);
        if (prior.isPresent()) return prior.get();

        List<Line> reserved = new ArrayList<>();
        try {
            BigDecimal total = BigDecimal.ZERO;
            List<Map<String, Object>> lines = new ArrayList<>();
            for (Line line : request.items()) {
                ProductClient.ProductResponse product = products.getProduct(line.productId());
                if (product == null || product.price() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product");
                products.reserveStock(line.productId(), internalToken, new ProductClient.QuantityRequest(line.quantity()));
                reserved.add(line);
                total = total.add(product.price().multiply(BigDecimal.valueOf(line.quantity())));
                lines.add(Map.of("productId", product.id(), "name", product.name() == null ? "Product" : product.name(), "unitPrice", product.price(), "quantity", line.quantity()));
            }
            return orders.save(new OrderRecord(claims.userId(), idempotency, total, mapper.writeValueAsString(lines)));
        } catch (Exception ex) {
            compensate(reserved);
            if (ex instanceof ResponseStatusException rse) throw rse;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout could not reserve stock or create the order; reserved quantities were compensated where possible");
        }
    }

    @GetMapping
    public List<OrderRecord> mine(@RequestHeader(value = "Authorization", required = false) String auth) {
        TokenClaims.Claims claims = tokens.require(auth);
        return orders.findByCustomerIdOrderByCreatedAtDesc(claims.userId());
    }

    @GetMapping("/{id}")
    public OrderRecord get(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable long id) {
        TokenClaims.Claims claims = tokens.require(auth);
        OrderRecord order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCustomerId().equals(claims.userId()) && !"ADMIN".equals(claims.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order belongs to another customer");
        return order;
    }

    @PostMapping("/{id}/payment-sandbox")
    @Transactional
    public OrderRecord paySandbox(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @PathVariable long id, @RequestParam(defaultValue = "success") String outcome) {
        TokenClaims.Claims claims = tokens.require(auth);
        OrderRecord order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCustomerId().equals(claims.userId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order belongs to another customer");
        if (!"PENDING_PAYMENT".equals(order.getStatus())) return order;
        if ("success".equalsIgnoreCase(outcome)) {
            order.paid("SANDBOX-" + UUID.randomUUID());
        } else {
            order.paymentFailed();
            releaseOrderStock(order);
        }
        return orders.save(order);
    }

    @PostMapping("/{id}/shipping-sandbox")
    @Transactional
    public OrderRecord shipSandbox(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable long id) {
        TokenClaims.Claims claims = tokens.require(auth);
        if (!"ADMIN".equals(claims.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        OrderRecord order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        try { order.shipped(); }
        catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
        return orders.save(order);
    }

    private void releaseOrderStock(OrderRecord order) {
        if (order.isStockReleased()) return;
        try {
            List<Map<String, Object>> lines = mapper.readValue(order.getItemsJson(), new TypeReference<>() {});
            for (Map<String, Object> line : lines) {
                Long productId = ((Number) line.get("productId")).longValue();
                int quantity = ((Number) line.get("quantity")).intValue();
                products.releaseStock(productId, internalToken, new ProductClient.QuantityRequest(quantity));
            }
            order.markStockReleased();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment failed but inventory compensation needs reconciliation");
        }
    }

    private void compensate(List<Line> reserved) {
        ListIterator<Line> iterator = reserved.listIterator(reserved.size());
        while (iterator.hasPrevious()) {
            Line line = iterator.previous();
            try { products.releaseStock(line.productId(), internalToken, new ProductClient.QuantityRequest(line.quantity())); }
            catch (Exception ignored) { /* surfaced in operational logs/reconciliation in a full outbox implementation */ }
        }
    }
}
