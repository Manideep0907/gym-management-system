
package com.gym.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "trainer_session_bookings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_member_session",
        columnNames = {"member_id", "session_id"}
    )
)
public class TrainerSessionBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 20)
    private String status = "BOOKED";

    @Column(nullable = false)
    private LocalDateTime bookedAt = LocalDateTime.now();

    public TrainerSessionBooking() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
}
