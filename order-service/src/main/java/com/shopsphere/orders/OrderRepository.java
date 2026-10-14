package com.shopsphere.orders;
import java.util.List;import java.util.Optional;import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<OrderRecord,Long>{Optional<OrderRecord> findByCustomerIdAndIdempotencyKey(Long customerId,String key);List<OrderRecord> findByCustomerIdOrderByCreatedAtDesc(Long customerId);}
