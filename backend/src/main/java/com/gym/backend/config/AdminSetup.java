
package com.gym.backend.config;

import com.gym.backend.entity.UserAccount;
import com.gym.backend.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSetup {

    @Bean
    CommandLineRunner createSecondAdmin(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            String email = "admin2@gym.com";

            if (repository.existsByEmail(email)) {
                System.out.println("Admin account already exists.");
                return;
            }

            UserAccount admin = new UserAccount();
            admin.setFullName("Gym Admin 2");
            admin.setEmail(email);
            admin.setPasswordHash(
                    passwordEncoder.encode("Admin@123"));
            admin.setRole("ADMIN");

            repository.save(admin);

            System.out.println(
                    "Admin created: admin2@gym.com with role ADMIN");
        };
    }
}
