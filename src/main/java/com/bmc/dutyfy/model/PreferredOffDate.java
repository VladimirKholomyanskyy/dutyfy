package com.bmc.dutyfy.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "preferred_off_dates")
public class PreferredOffDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate offDate;

    public PreferredOffDate() {
    }

    public PreferredOffDate(Employee employee, LocalDate offDate) {
        this.employee = employee;
        this.offDate = offDate;
    }

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

    public LocalDate getOffDate() {
        return offDate;
    }

    public void setOffDate(LocalDate offDate) {
        this.offDate = offDate;
    }

    @Override
    public String toString() {
        return "PreferredOffDates{" +
                "id=" + id +
                ", employee=" + employee +
                ", offDate=" + offDate +
                '}';
    }
}
