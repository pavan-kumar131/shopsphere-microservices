package com.shopsphere.product;

import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ProductSeed {
    @Bean ApplicationRunner seedProducts(ProductRepository products, @Value("${storage.cdn.base-url:}") String cdnBase) {
        return args -> {
            if (products.count() == 0) {
                products.save(new Product("Everyday Backpack", "A durable daily carry with room for the essentials.", new BigDecimal("1499.00"), "Accessories", image(cdnBase, "backpack.svg"), 25));
                products.save(new Product("Wireless Headphones", "Comfortable sound for focus, travel and downtime.", new BigDecimal("2999.00"), "Electronics", image(cdnBase, "headphones.svg"), 18));
                products.save(new Product("Classic Sneakers", "Minimal design and everyday comfort.", new BigDecimal("2499.00"), "Fashion", image(cdnBase, "sneakers.svg"), 32));
                products.save(new Product("Ceramic Coffee Set", "A simple ceramic set for slow mornings.", new BigDecimal("899.00"), "Home", image(cdnBase, "coffee-set.svg"), 12));
                products.save(new Product("Desk Lamp", "Warm, focused light for your workspace.", new BigDecimal("1799.00"), "Home", image(cdnBase, "desk-lamp.svg"), 9));
            }
        };
    }
    private static String image(String base, String file) { return base == null || base.isBlank() ? null : base.replaceAll("/$", "") + "/seed/" + file; }
}
