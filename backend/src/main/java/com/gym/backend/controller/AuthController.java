
package com.gym.backend.controller;

import com.gym.backend.entity.UserAccount;
import com.gym.backend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }

        if (request.getPassword() == null
                || request.getPassword().length() < 8
                || request.getPassword().length() > 72) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Password must contain 8 to 72 characters"));
        }

        String email = request.getEmail().trim().toLowerCase();

        if (repository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error",
                            "An account with this email already exists"));
        }

        UserAccount account = new UserAccount();
        account.setFullName(
                request.getFullName() == null
                        ? null : request.getFullName().trim());
        account.setEmail(email);
        account.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));
        account.setRole("USER");

        UserAccount saved = repository.save(account);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Registration successful",
                        "id", saved.getId(),
                        "fullName", saved.getFullName() == null
                                ? "" : saved.getFullName(),
                        "email", saved.getEmail(),
                        "role", saved.getRole()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Email and password are required"));
        }

        String email = request.getEmail().trim().toLowerCase();

        return repository.findByEmail(email)
                .filter(account -> passwordEncoder.matches(
                        request.getPassword(), account.getPasswordHash()))
                .<ResponseEntity<?>>map(account ->
                        ResponseEntity.ok(Map.of(
                                "message", "Login successful",
                                "id", account.getId(),
                                "fullName", account.getFullName() == null
                                        ? "" : account.getFullName(),
                                "email", account.getEmail(),
                                "role", account.getRole()
                        )))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error",
                                        "Invalid email or password")));
    }

    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
