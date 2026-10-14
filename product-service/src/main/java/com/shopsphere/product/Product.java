package com.shopsphere.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 1000) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 80) private String category;
    @Column(length = 1000) private String imageUrl;
    @Column(nullable = false) private Integer stock = 0;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "seller_id") private Long sellerId;

    protected Product() {}
    public Product(String name, String description, BigDecimal price, String category, String imageUrl, Integer stock) {
        this(name, description, price, category, imageUrl, stock, null);
    }
    public Product(String name, String description, BigDecimal price, String category, String imageUrl, Integer stock, Long sellerId) {
        this.name = name; this.description = description; this.price = price; this.category = category;
        this.imageUrl = imageUrl; this.stock = stock == null ? 0 : stock; this.createdAt = Instant.now(); this.sellerId = sellerId;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public Integer getStock() { return stock; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getSellerId() { return sellerId; }
    public void update(String name, String description, BigDecimal price, String category, String imageUrl, Integer stock) {
        this.name = name; this.description = description; this.price = price; this.category = category;
        this.imageUrl = imageUrl; this.stock = stock == null ? 0 : stock;
    }
}
