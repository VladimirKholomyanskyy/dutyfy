package com.bmc.dutyfy.repository;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAll(Specification<Employee> spec);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByActiveTrue();

    List<Employee> findByRole(UserRole role);

    @Query("SELECT e FROM Employee e WHERE e.active = true AND e.role = :role")
    List<Employee> findActiveEmployeesByRole(@Param("role") UserRole role);
}