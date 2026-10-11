package com.gym.backend.repository;

import com.gym.backend.entity.MemberMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {
    List<MemberMembership> findByMemberIdOrderByIdDesc(Long memberId);
    boolean existsByMemberIdAndStatusIn(Long memberId, List<String> statuses);
    List<MemberMembership> findByMemberIdAndStatusIn(Long memberId, List<String> statuses);
}
