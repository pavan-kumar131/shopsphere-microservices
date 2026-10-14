package com.shopsphere.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
    int releaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
