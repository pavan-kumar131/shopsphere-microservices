package com.shopsphere.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.regions.Region;

@SpringBootApplication
public class ProductServiceApplication {
    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner() { return S3Presigner.builder().region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-south-1"))).build(); }
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
