
package com.gym.backend.repository;

import com.gym.backend.entity.TrainerSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerSessionRepository
        extends JpaRepository<TrainerSession, Long> {
}
