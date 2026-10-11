package com.gym.backend.controller;

import com.gym.backend.entity.Member;
import com.gym.backend.entity.MemberMembership;
import com.gym.backend.entity.MembershipPlan;
import com.gym.backend.repository.MemberMembershipRepository;
import com.gym.backend.repository.MemberRepository;
import com.gym.backend.repository.MembershipPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member-memberships")
@CrossOrigin(origins = "http://localhost:5173")
public class MemberMembershipController {
    private static final List<String> BLOCKING_STATUSES = List.of("ACTIVE", "PENDING_PAYMENT");
    private final MemberMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;

    public MemberMembershipController(MemberMembershipRepository membershipRepository, MemberRepository memberRepository, MembershipPlanRepository planRepository) {
        this.membershipRepository = membershipRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
    }

    @PostMapping
    public ResponseEntity<?> selectPlan(@RequestBody SelectionRequest request) {
        if (request.getMemberId() == null || request.getPlanId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Member ID and plan ID are required"));
        }
        Member member = memberRepository.findById(request.getMemberId()).orElse(null);
        if (member == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Member not found"));
        MembershipPlan plan = planRepository.findById(request.getPlanId()).orElse(null);
        if (plan == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Membership plan not found"));
        if (!plan.isActive()) return ResponseEntity.badRequest().body(Map.of("error", "This membership plan is inactive"));
        if (plan.getDurationMonths() == null || plan.getDurationMonths() <= 0) return ResponseEntity.badRequest().body(Map.of("error", "The membership plan duration is invalid"));

        if (membershipRepository.existsByMemberIdAndStatusIn(member.getId(), BLOCKING_STATUSES)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "You already have an active or payment-pending membership. You cannot select another plan."));
        }

        LocalDate startDate = LocalDate.now();
        MemberMembership membership = new MemberMembership();
        membership.setMemberId(member.getId());
        membership.setPlanId(plan.getId());
        membership.setStartDate(startDate);
        membership.setEndDate(startDate.plusMonths(plan.getDurationMonths()));
        membership.setStatus("PENDING_PAYMENT");
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipRepository.save(membership));
    }

    @GetMapping
    public ResponseEntity<?> getMemberships(@RequestParam(required = false) Long memberId) {
        if (memberId == null) return ResponseEntity.ok(membershipRepository.findAll());
        if (!memberRepository.existsById(memberId)) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Member not found"));
        return ResponseEntity.ok(membershipRepository.findByMemberIdOrderByIdDesc(memberId));
    }

    public static class SelectionRequest {
        private Long memberId;
        private Long planId;
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }
}
