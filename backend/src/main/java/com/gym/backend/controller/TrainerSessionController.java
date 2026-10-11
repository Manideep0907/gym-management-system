
package com.gym.backend.controller;

import com.gym.backend.entity.*;
import com.gym.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainer-sessions")
@CrossOrigin(origins = "http://localhost:5173")
public class TrainerSessionController {

    private final TrainerSessionRepository sessionRepository;
    private final TrainerSessionBookingRepository bookingRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;

    public TrainerSessionController(
            TrainerSessionRepository sessionRepository,
            TrainerSessionBookingRepository bookingRepository,
            TrainerRepository trainerRepository,
            MemberRepository memberRepository) {
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.trainerRepository = trainerRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public List<TrainerSession> getSessions() {
        return sessionRepository.findAll().stream()
                .filter(TrainerSession::isActive)
                .toList();
    }

    @GetMapping("/all")
    public List<TrainerSession> getAllSessionsForAdmin() {
        return sessionRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createSession(
            @RequestBody SessionRequest request) {
        ResponseEntity<?> error = validate(request);
        if (error != null) return error;

        TrainerSession session = new TrainerSession();
        applyRequest(session, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionRepository.save(session));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSession(
            @PathVariable Long id,
            @RequestBody SessionRequest request) {
        TrainerSession session = sessionRepository.findById(id).orElse(null);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found"));
        }

        ResponseEntity<?> error = validate(request);
        if (error != null) return error;

        applyRequest(session, request);
        return ResponseEntity.ok(sessionRepository.save(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        if (!sessionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found"));
        }

        // Preserve booking history by marking the session inactive.
        TrainerSession session = sessionRepository.findById(id).orElseThrow();
        session.setActive(false);
        sessionRepository.save(session);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> bookSession(
            @RequestBody BookingRequest request) {
        if (request.getMemberId() == null || request.getSessionId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Member ID and session ID are required"));
        }

        if (!memberRepository.existsById(request.getMemberId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Member not found"));
        }

        TrainerSession session = sessionRepository
                .findById(request.getSessionId()).orElse(null);

        if (session == null || !session.isActive()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Active session not found"));
        }

        if (bookingRepository.existsByMemberIdAndSessionId(
                request.getMemberId(), request.getSessionId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Member has already booked this session"));
        }

        long bookedCount = bookingRepository.findAll().stream()
                .filter(b -> b.getSessionId().equals(session.getId()))
                .filter(b -> "BOOKED".equals(b.getStatus()))
                .count();

        if (bookedCount >= session.getCapacity()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "This session is full"));
        }

        TrainerSessionBooking booking = new TrainerSessionBooking();
        booking.setMemberId(request.getMemberId());
        booking.setSessionId(request.getSessionId());
        booking.setStatus("BOOKED");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingRepository.save(booking));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings(
            @RequestParam(required = false) Long memberId) {
        if (memberId == null) {
            return ResponseEntity.ok(bookingRepository.findAll());
        }

        if (!memberRepository.existsById(memberId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Member not found"));
        }

        return ResponseEntity.ok(
                bookingRepository.findByMemberIdOrderByBookedAtDesc(memberId));
    }

    private ResponseEntity<?> validate(SessionRequest request) {
        if (request.getName() == null || request.getName().isBlank()
                || request.getTrainerId() == null
                || request.getSessionDay() == null
                || request.getStartTime() == null
                || request.getEndTime() == null
                || request.getCapacity() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "All required session fields must be provided"));
        }

        if (!trainerRepository.existsById(request.getTrainerId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Trainer not found"));
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "End time must be after start time"));
        }

        if (request.getCapacity() < 1 || request.getCapacity() > 200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Capacity must be between 1 and 200"));
        }

        return null;
    }

    private void applyRequest(
            TrainerSession session, SessionRequest request) {
        session.setName(request.getName().trim());
        session.setDescription(request.getDescription());
        session.setTrainerId(request.getTrainerId());
        session.setSessionDay(request.getSessionDay().trim());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCapacity(request.getCapacity());
        session.setActive(request.getActive() == null || request.getActive());
    }

    public static class SessionRequest {
        private String name;
        private String description;
        private Long trainerId;
        private String sessionDay;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer capacity;
        private Boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long getTrainerId() { return trainerId; }
        public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

        public String getSessionDay() { return sessionDay; }
        public void setSessionDay(String sessionDay) { this.sessionDay = sessionDay; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class BookingRequest {
        private Long memberId;
        private Long sessionId;

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    }
}
