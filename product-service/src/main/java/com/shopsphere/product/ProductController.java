package com.shopsphere.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    public record ProductRequest(
        @NotBlank @Size(max=120) String name,
        @NotBlank @Size(max=1000) String description,
        @NotNull @DecimalMin(value="0.01") BigDecimal price,
        @NotBlank @Size(max=80) String category,
        @Size(max=1000) String imageUrl,
        @NotNull @PositiveOrZero Integer stock
    ) {}

    private final ProductRepository products;
    private final JwtAdminVerifier verifier;
    public ProductController(ProductRepository products, JwtAdminVerifier verifier) { this.products = products; this.verifier = verifier; }

    @GetMapping public List<Product> list() { return products.findAll(); }
    @GetMapping("/{id}") public Product get(@PathVariable long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestHeader(value="Authorization", required=false) String auth, @Valid @RequestBody ProductRequest request) {
        var claims = verifier.requireClaims(auth);
        String role = claims.get("role", String.class);
        if (!"SELLER".equals(role) && !"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller or administrator access required");
        Long sellerId = "SELLER".equals(role) ? parseSubject(claims.getSubject()) : null;
        return products.save(new Product(request.name().trim(), request.description().trim(), request.price(), request.category().trim(), request.imageUrl(), request.stock(), sellerId));
    }
    @PutMapping("/{id}")
    public Product update(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable long id, @Valid @RequestBody ProductRequest request) {
        var claims = verifier.requireClaims(auth);
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        enforceOwnerOrAdmin(claims, p);
        p.update(request.name().trim(), request.description().trim(), request.price(), request.category().trim(), request.imageUrl(), request.stock());
        return products.save(p);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable long id) {
        var claims = verifier.requireClaims(auth);
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        enforceOwnerOrAdmin(claims, p);
        products.delete(p);
    }

    private void enforceOwnerOrAdmin(io.jsonwebtoken.Claims claims, Product product) {
        String role = claims.get("role", String.class);
        if ("ADMIN".equals(role)) return;
        if (!"SELLER".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller or administrator access required");
        Long userId = parseSubject(claims.getSubject());
        if (product.getSellerId() == null || !product.getSellerId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage products you own");
    }

    private Long parseSubject(String subject) {
        try { return Long.valueOf(subject); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token subject"); }
    }
}
