package com.bmc.dutyfy.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_swap_requests")
@EntityListeners(AuditingEntityListener.class)
public class ShiftSwapRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwapStatus status = SwapStatus.PENDING;

    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestDate;

    private LocalDateTime responseDate;

    public ShiftSwapRequest() {
    }

    public ShiftSwapRequest(Employee requester, Employee targetEmployee, Shift shift, String reason) {
        this.requester = requester;
        this.targetEmployee = targetEmployee;
        this.shift = shift;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getRequester() {
        return requester;
    }

    public void setRequester(Employee requester) {
        this.requester = requester;
    }

    public Employee getTargetEmployee() {
        return targetEmployee;
    }

    public void setTargetEmployee(Employee targetEmployee) {
        this.targetEmployee = targetEmployee;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public SwapStatus getStatus() {
        return status;
    }

    public void setStatus(SwapStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDateTime responseDate) {
        this.responseDate = responseDate;
    }

    @Override
    public String toString() {
        return "ShiftSwapRequest{" +
                "id=" + id +
                ", requester=" + requester.getName() +
                ", targetEmployee=" + targetEmployee.getName() +
                ", shift=" + shift.getShiftDate() +
                ", status=" + status +
                ", reason='" + reason + '\'' +
                '}';
    }
}