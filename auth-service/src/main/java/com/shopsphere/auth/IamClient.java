package com.shopsphere.auth;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "iam-service", url = "${app.iam-url}", dismiss404 = true)
public interface IamClient {
    record IamUser(Long id, String email, List<String> roles, boolean active) {}
    record CreateUserRequest(String email, List<String> roles) {}
    @GetMapping("/internal/users/by-email") IamUser findByEmail(@RequestParam("email") String email);
    @PostMapping("/internal/users") IamUser create(@RequestBody CreateUserRequest request);
}
