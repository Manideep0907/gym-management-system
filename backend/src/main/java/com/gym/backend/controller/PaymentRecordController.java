
package com.gym.backend.controller;

import com.gym.backend.entity.Member;
import com.gym.backend.entity.MemberMembership;
import com.gym.backend.entity.Payment;
import com.gym.backend.repository.MemberMembershipRepository;
import com.gym.backend.repository.MemberRepository;
import com.gym.backend.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-records")
@CrossOrigin(origins = {"http://localhost:5173", "${FRONTEND_URL:http://localhost:5173}"})
public class PaymentRecordController {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MemberMembershipRepository membershipRepository;

    public PaymentRecordController(
            PaymentRepository paymentRepository,
            MemberRepository memberRepository,
            MemberMembershipRepository membershipRepository) {
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping
    public ResponseEntity<?> recordPayment(
            @RequestBody PaymentRequest request) {

        if (request.getMemberId() == null
                || request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "A valid member ID and positive amount are required"));
        }

        if (request.getMethod() == null
                || !(request.getMethod().equalsIgnoreCase("ONLINE")
                || request.getMethod().equalsIgnoreCase("OFFLINE"))) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Method must be ONLINE or OFFLINE"));
        }

        if (request.getReference() == null
                || request.getReference().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Payment reference is required"));
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElse(null);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Member not found"));
        }

        var memberships = membershipRepository
                .findByMemberIdOrderByIdDesc(member.getId());

        if (memberships.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Select a membership plan before recording payment"));
        }

        MemberMembership membership = memberships.get(0);

        String method = request.getMethod().toUpperCase();
        String status;

        if ("ONLINE".equals(method)) {
            status = "PENDING";
        } else {
            if (request.getOfflineConfirmed() == null
                    || !request.getOfflineConfirmed()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error",
                                "Offline payment must be confirmed by an administrator"));
            }
            status = "COMPLETED";
        }

        Payment payment = new Payment();
        payment.setMemberId(member.getId());
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(LocalDate.now());
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setReference(request.getReference().trim());

        Payment saved = paymentRepository.save(payment);

        if ("COMPLETED".equals(status)) {
            membership.setStatus("ACTIVE");
            membershipRepository.save(membership);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<?> getPayments(
            @RequestParam(required = false) Long memberId) {

        if (memberId == null) {
            return ResponseEntity.ok(paymentRepository.findAll());
        }

        if (!memberRepository.existsById(memberId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Member not found"));
        }

        return ResponseEntity.ok(
                paymentRepository.findAll().stream()
                        .filter(payment -> memberId.equals(payment.getMemberId()))
                        .toList());
    }

    public static class PaymentRequest {
        private Long memberId;
        private BigDecimal amount;
        private String method;
        private String reference;
        private Boolean offlineConfirmed;

        public Long getMemberId() {
            return memberId;
        }

        public void setMemberId(Long memberId) {
            this.memberId = memberId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public Boolean getOfflineConfirmed() {
            return offlineConfirmed;
        }

        public void setOfflineConfirmed(Boolean offlineConfirmed) {
            this.offlineConfirmed = offlineConfirmed;
        }
    }
}
