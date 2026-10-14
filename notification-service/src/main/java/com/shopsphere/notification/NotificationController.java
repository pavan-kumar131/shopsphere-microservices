package com.shopsphere.notification;
import jakarta.validation.Valid;import jakarta.validation.constraints.Email;import jakarta.validation.constraints.NotBlank;import java.time.Instant;import java.util.List;import java.util.UUID;import java.util.concurrent.CopyOnWriteArrayList;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/notifications") public class NotificationController {
 public record Request(@Email String recipient,@NotBlank String subject,@NotBlank String message){} public record Notification(String id,String recipient,String subject,String message,String status,Instant createdAt){}
 private final CopyOnWriteArrayList<Notification> sent=new CopyOnWriteArrayList<>();
 @PostMapping public Notification submit(@Valid @RequestBody Request r){Notification n=new Notification(UUID.randomUUID().toString(),r.recipient(),r.subject(),r.message(),"ACCEPTED_FOR_LOCAL_DEMO",Instant.now());sent.add(n);return n;}
 @GetMapping public List<Notification> list(){return List.copyOf(sent);}
}
