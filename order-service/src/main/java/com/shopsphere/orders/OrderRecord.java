package com.shopsphere.orders;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="shop_orders", uniqueConstraints=@UniqueConstraint(name="uk_order_idempotency", columnNames={"customerId","idempotencyKey"}))
public class OrderRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long customerId;
 @Column(nullable=false,length=100) private String idempotencyKey;
 @Column(nullable=false,length=30) private String status="PENDING_PAYMENT";
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
 @Column(nullable=false,columnDefinition="text") private String itemsJson;
 @Column(length=120) private String paymentReference;
 @Column(nullable=false,length=40) private String shippingStatus="NOT_CREATED";
 @Column(nullable=false) private Instant createdAt=Instant.now();
 @Column(nullable=false) private boolean stockReleased=false;
 protected OrderRecord() {}
 public OrderRecord(Long customerId,String key,BigDecimal total,String itemsJson){this.customerId=customerId;this.idempotencyKey=key;this.total=total;this.itemsJson=itemsJson;}
 public Long getId(){return id;} public Long getCustomerId(){return customerId;} public String getIdempotencyKey(){return idempotencyKey;} public String getStatus(){return status;} public BigDecimal getTotal(){return total;} public String getItemsJson(){return itemsJson;} public String getPaymentReference(){return paymentReference;} public String getShippingStatus(){return shippingStatus;} public Instant getCreatedAt(){return createdAt;} public boolean isStockReleased(){return stockReleased;} public void markStockReleased(){stockReleased=true;}
 public void paid(String ref){status="PAID";paymentReference=ref;shippingStatus="PROCESSING";} public void paymentFailed(){status="PAYMENT_FAILED";} public void shipped(){if(!"PAID".equals(status)) throw new IllegalStateException("Only paid orders can ship");shippingStatus="SHIPPED";}
}
