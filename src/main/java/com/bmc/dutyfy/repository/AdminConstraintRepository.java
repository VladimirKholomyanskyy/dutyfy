package com.bmc.dutyfy.repository;

import com.bmc.dutyfy.model.AdminConstraint;
import com.bmc.dutyfy.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdminConstraintRepository extends JpaRepository<AdminConstraint, Long> {

    List<AdminConstraint> findByEmployeeAndConstraintDateBetween(Employee employee, LocalDate startDate,
                                                                 LocalDate endDate);

    @Query("SELECT ac FROM AdminConstraint ac WHERE YEAR(ac.constraintDate) = :year")
    List<AdminConstraint> findByYear(@Param("year") int year);

    List<AdminConstraint> findByConstraintDateBetween(LocalDate startDate, LocalDate endDate);
}
