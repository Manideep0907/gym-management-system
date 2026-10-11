
package com.gym.backend.repository;

import com.gym.backend.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository
        extends JpaRepository<MembershipPlan, Long> {
}
