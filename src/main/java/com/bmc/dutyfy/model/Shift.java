package com.bmc.dutyfy.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "shifts")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate shiftDate;

    public Shift() {
    }

    public Shift(Employee employee, LocalDate shiftDate) {
        this.employee = employee;
        this.shiftDate = shiftDate;
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

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    @Override
    public String toString() {
        return "Shifts{" +
                "id=" + id +
                ", employee=" + employee +
                ", shiftDate=" + shiftDate +
                '}';
    }
}
