package com.shopsphere.product;

import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {
  private final JwtAdminVerifier verifier;
  private final S3Presigner presigner;
  private final String bucket;
  private final String cdnBase;
  public record UploadRequest(String fileName, String contentType) {}
  public ProductImageController(JwtAdminVerifier verifier, S3Presigner presigner,
      @Value("${storage.s3.bucket:}") String bucket,
      @Value("${storage.cdn.base-url:}") String cdnBase) {
    this.verifier=verifier; this.presigner=presigner; this.bucket=bucket; this.cdnBase=cdnBase;
  }
  @PostMapping("/image-upload-url")
  public Map<String,String> createUploadUrl(@RequestHeader(value="Authorization", required=false) String authorization,
                                            @RequestBody UploadRequest request) {
    verifier.requireSellerOrAdmin(authorization);
    if (bucket.isBlank() || cdnBase.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product image storage is not configured");
    if (request == null || request.fileName() == null || request.contentType() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"fileName and contentType are required");
    String type=request.contentType().toLowerCase(Locale.ROOT);
    if (!java.util.Set.of("image/jpeg","image/png","image/webp").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only JPEG, PNG and WebP images are allowed");
    String extension=switch(type){case "image/jpeg"->"jpg";case "image/png"->"png";default->"webp";};
    String key="products/"+UUID.randomUUID()+"."+extension;
    URL url=presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(5))
      .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build()).build()).url();
    return Map.of("uploadUrl",url.toString(),"imageUrl",cdnBase.replaceAll("/$","")+"/"+key,"objectKey",key);
  }
}
