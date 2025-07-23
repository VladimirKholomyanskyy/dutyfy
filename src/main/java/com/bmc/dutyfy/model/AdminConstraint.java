package com.bmc.dutyfy.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_constraints")
@EntityListeners(AuditingEntityListener.class)
public class AdminConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate constraintDate;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private boolean isFlexible = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    public AdminConstraint() {
    }

    public AdminConstraint(Employee employee, LocalDate constraintDate, String reason, boolean isFlexible) {
        this.employee = employee;
        this.constraintDate = constraintDate;
        this.reason = reason;
        this.isFlexible = isFlexible;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getConstraintDate() {
        return constraintDate;
    }

    public void setConstraintDate(LocalDate constraintDate) {
        this.constraintDate = constraintDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isFlexible() {
        return isFlexible;
    }

    public void setFlexible(boolean flexible) {
        isFlexible = flexible;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public String toString() {
        return "AdminConstraint{" +
                "id=" + id +
                ", employee=" + employee.getName() +
                ", constraintDate=" + constraintDate +
                ", reason='" + reason + '\'' +
                ", isFlexible=" + isFlexible +
                '}';
    }
}