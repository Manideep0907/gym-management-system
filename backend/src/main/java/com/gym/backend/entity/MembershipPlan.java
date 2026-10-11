
package com.gym.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public MembershipPlan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
