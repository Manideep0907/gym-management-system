
package com.gym.backend.repository;

import com.gym.backend.entity.TrainerSessionBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainerSessionBookingRepository
        extends JpaRepository<TrainerSessionBooking, Long> {

    boolean existsByMemberIdAndSessionId(Long memberId, Long sessionId);

    List<TrainerSessionBooking> findByMemberIdOrderByBookedAtDesc(Long memberId);
}
