
package com.gym.backend.controller;

import com.gym.backend.entity.Member;
import com.gym.backend.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "${FRONTEND_URL:http://localhost:5173}"
})
public class MemberController {

    private final MemberRepository repository;

    public MemberController(MemberRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Member> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Member> create(@RequestBody Member member) {
        member.setId(null);
        Member saved = repository.save(member);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> update(
            @PathVariable Long id,
            @RequestBody Member updated) {

        System.out.println(
                "PROFILE UPDATE REQUEST RECEIVED for member ID: " + id
        );

        return repository.findById(id).map(member -> {
            if (updated.getFullName() != null) {
                member.setFullName(updated.getFullName().trim());
            }

            if (updated.getEmail() != null) {
                member.setEmail(updated.getEmail().trim());
            }

            member.setPhone(updated.getPhone());
            member.setGender(updated.getGender());
            member.setJoinDate(updated.getJoinDate());
            member.setAge(updated.getAge());
            member.setHeightCm(updated.getHeightCm());
            member.setWeightKg(updated.getWeightKg());
            member.setFitnessGoal(updated.getFitnessGoal());
            member.setActive(updated.isActive());

            Member saved = repository.save(member);

            System.out.println(
                    "PROFILE UPDATE SAVED for member ID: " + saved.getId()
            );

            return ResponseEntity.ok(saved);
        }).orElseGet(() -> {
            System.out.println(
                    "PROFILE UPDATE FAILED: member not found, ID: " + id
            );
            return ResponseEntity.notFound().build();
        });
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
