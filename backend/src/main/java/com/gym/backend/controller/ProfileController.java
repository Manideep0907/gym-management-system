
package com.gym.backend.controller;

import com.gym.backend.entity.Member;
import com.gym.backend.entity.UserAccount;
import com.gym.backend.repository.MemberRepository;
import com.gym.backend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:5173", "${FRONTEND_URL:http://localhost:5173}"})
public class ProfileController {

    private final MemberRepository memberRepository;
    private final UserAccountRepository accountRepository;

    public ProfileController(
            MemberRepository memberRepository,
            UserAccountRepository accountRepository) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (accountRepository.findByEmail(normalizedEmail).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User account not found"));
        }

        Optional<Member> member = memberRepository.findAll().stream()
                .filter(m -> normalizedEmail.equalsIgnoreCase(m.getEmail()))
                .findFirst();

        return member.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "message",
                                "Profile not created yet. Submit your profile first."
                        )));
    }

    @PutMapping
    public ResponseEntity<?> saveProfile(@RequestBody ProfileRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }

        String email = request.getEmail().trim().toLowerCase();

        UserAccount account = accountRepository.findByEmail(email).orElse(null);

        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Register an account before saving a profile"));
        }

        if (request.getAge() != null
                && (request.getAge() < 1 || request.getAge() > 120)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Age must be between 1 and 120"));
        }

        if (request.getHeightCm() != null
                && (request.getHeightCm() <= 0 || request.getHeightCm() > 300)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Height must be between 0 and 300 cm"));
        }

        if (request.getWeightKg() != null
                && (request.getWeightKg() <= 0 || request.getWeightKg() > 700)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Weight must be between 0 and 700 kg"));
        }

        Member member = memberRepository.findAll().stream()
                .filter(m -> email.equalsIgnoreCase(m.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    Member newMember = new Member();
                    newMember.setEmail(email);
                    newMember.setJoinDate(LocalDate.now());
                    newMember.setActive(true);
                    return newMember;
                });

        String fullName = request.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = account.getFullName();
        }
        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Full name is required"));
        }

        member.setFullName(fullName.trim());
        member.setPhone(request.getPhone());
        member.setGender(request.getGender());
        member.setAge(request.getAge());
        member.setHeightCm(request.getHeightCm());
        member.setWeightKg(request.getWeightKg());
        member.setFitnessGoal(request.getFitnessGoal());

        Member saved = memberRepository.save(member);

        return ResponseEntity.ok(saved);
    }

    public static class ProfileRequest {
        private String email;
        private String fullName;
        private String phone;
        private String gender;
        private Integer age;
        private Double heightCm;
        private Double weightKg;
        private String fitnessGoal;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Double getHeightCm() {
            return heightCm;
        }

        public void setHeightCm(Double heightCm) {
            this.heightCm = heightCm;
        }

        public Double getWeightKg() {
            return weightKg;
        }

        public void setWeightKg(Double weightKg) {
            this.weightKg = weightKg;
        }

        public String getFitnessGoal() {
            return fitnessGoal;
        }

        public void setFitnessGoal(String fitnessGoal) {
            this.fitnessGoal = fitnessGoal;
        }
    }
}
