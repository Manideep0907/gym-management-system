
package com.gym.backend.controller;

import com.gym.backend.entity.Trainer;
import com.gym.backend.repository.TrainerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainers")
@CrossOrigin(origins = "http://localhost:5173")
public class TrainerController {

    private final TrainerRepository repository;

    public TrainerController(TrainerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Trainer> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Trainer trainer) {

        if (trainer.getEmail() == null ||
                trainer.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Trainer email is required")
            );
        }

        if (repository.existsByEmail(trainer.getEmail().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error",
                            "A trainer with this email already exists")
            );
        }

        trainer.setId(null);
        trainer.setEmail(trainer.getEmail().trim());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repository.save(trainer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Trainer updated) {

        return repository.findById(id).<ResponseEntity<?>>map(trainer -> {

            if (updated.getEmail() == null ||
                    updated.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Trainer email is required")
                );
            }

            String email = updated.getEmail().trim();

            if (repository.existsByEmailAndIdNot(email, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        Map.of("error",
                                "Another trainer already uses this email")
                );
            }

            trainer.setFullName(updated.getFullName());
            trainer.setEmail(email);
            trainer.setPhone(updated.getPhone());
            trainer.setSpecialization(updated.getSpecialization());
            trainer.setHireDate(updated.getHireDate());
            trainer.setActive(updated.isActive());

            return ResponseEntity.ok(repository.save(trainer));

        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
