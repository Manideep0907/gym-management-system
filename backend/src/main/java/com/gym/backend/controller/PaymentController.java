package com.gym.backend.controller;

import com.gym.backend.entity.MemberMembership;
import com.gym.backend.entity.Payment;
import com.gym.backend.repository.MemberMembershipRepository;
import com.gym.backend.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {"http://localhost:5173", "${FRONTEND_URL:http://localhost:5173}"})
public class PaymentController {

    private final PaymentRepository repository;
    private final MemberMembershipRepository membershipRepository;

    private static final List<String> VALID_METHODS =
            List.of("ONLINE", "OFFLINE");

    private static final List<String> VALID_STATUSES =
            List.of("PENDING", "COMPLETED", "FAILED", "REFUNDED");

    public PaymentController(
            PaymentRepository repository,
            MemberMembershipRepository membershipRepository) {
        this.repository = repository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    public List<Payment> getAll(
            @RequestParam(required = false) Long memberId) {

        List<Payment> all = repository.findAll();

        if (memberId == null) {
            return all;
        }

        return all.stream()
                .filter(payment -> memberId.equals(payment.getMemberId()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Payment payment) {

        if (payment.getMemberId() == null) {
            return badRequest("Member ID is required.");
        }

        if (!validAmount(payment.getAmount())) {
            return badRequest("Amount must be greater than zero.");
        }

        String method = normalize(payment.getMethod());
        String status = normalize(payment.getStatus());

        if (!VALID_METHODS.contains(method)) {
            return badRequest("Payment method must be ONLINE or OFFLINE.");
        }

        if (!VALID_STATUSES.contains(status)) {
            return badRequest(
                    "Payment status must be PENDING, COMPLETED, FAILED or REFUNDED.");
        }

        payment.setId(null);
        payment.setMethod(method);
        payment.setStatus(status);

        Payment saved = repository.saveAndFlush(payment);

        synchronizeMembership(saved.getMemberId(), status);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Payment updated) {

        Payment existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (!validAmount(updated.getAmount())) {
            return badRequest("Amount must be greater than zero.");
        }

        String method = normalize(updated.getMethod());
        String status = normalize(updated.getStatus());

        if (!VALID_METHODS.contains(method)) {
            return badRequest("Payment method must be ONLINE or OFFLINE.");
        }

        if (!VALID_STATUSES.contains(status)) {
            return badRequest(
                    "Payment status must be PENDING, COMPLETED, FAILED or REFUNDED.");
        }

        // Preserve the existing member association.
        // Editing a payment must not accidentally transfer it to another member.
        Long memberId = existing.getMemberId();

        existing.setAmount(updated.getAmount());
        existing.setPaymentDate(updated.getPaymentDate());
        existing.setMethod(method);
        existing.setStatus(status);
        existing.setReference(updated.getReference());

        Payment saved = repository.saveAndFlush(existing);

        // Save the payment and its related membership status in one transaction.
        synchronizeMembership(memberId, status);

        return ResponseEntity.ok(saved);
    }

    private void synchronizeMembership(Long memberId, String paymentStatus) {

        List<MemberMembership> memberships =
                membershipRepository.findByMemberIdOrderByIdDesc(memberId);

        if (memberships.isEmpty()) {
            return;
        }

        MemberMembership latest = memberships.get(0);

        String membershipStatus = switch (paymentStatus) {
            case "COMPLETED" -> "ACTIVE";
            case "PENDING" -> "PENDING_PAYMENT";
            case "FAILED" -> "PAYMENT_FAILED";
            case "REFUNDED" -> "REFUNDED";
            default -> "PENDING_PAYMENT";
        };

        latest.setStatus(membershipStatus);
        membershipRepository.save(latest);

        // Retain older database records without leaving duplicate
        // active or pending memberships.
        for (int i = 1; i < memberships.size(); i++) {
            MemberMembership older = memberships.get(i);
            String oldStatus = normalize(older.getStatus());

            if ("ACTIVE".equals(oldStatus)
                    || "PENDING_PAYMENT".equals(oldStatus)) {
                older.setStatus("SUPERSEDED");
                membershipRepository.save(older);
            }
        }
    }

    private boolean validAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}