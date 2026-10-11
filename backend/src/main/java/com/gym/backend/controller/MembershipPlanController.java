
package com.gym.backend.controller;

import com.gym.backend.entity.MembershipPlan;
import com.gym.backend.repository.MembershipPlanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membership-plans")
@CrossOrigin(origins = "http://localhost:5173")
public class MembershipPlanController {

    private final MembershipPlanRepository repository;

    public MembershipPlanController(MembershipPlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MembershipPlan> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembershipPlan> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MembershipPlan> create(
            @RequestBody MembershipPlan plan) {
        plan.setId(null);
        return ResponseEntity.status(201).body(repository.save(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembershipPlan> update(
            @PathVariable Long id,
            @RequestBody MembershipPlan updated) {

        return repository.findById(id)
                .map(plan -> {
                    plan.setName(updated.getName());
                    plan.setDurationMonths(updated.getDurationMonths());
                    plan.setPrice(updated.getPrice());
                    plan.setDescription(updated.getDescription());
                    plan.setActive(updated.isActive());

                    MembershipPlan saved = repository.save(plan);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
