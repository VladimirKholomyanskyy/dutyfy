package com.bmc.dutyfy.repository;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.PreferredOffDate;
import com.bmc.dutyfy.model.Shift;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecificationFactory {
    public static Specification<Employee> hasShiftsInYear(int year) {
        return (root, query, criteriaBuilder) -> {
            // Join with shifts
            Join<Employee, Shift> shifts = root.join("shifts");
            // Create a predicate for filtering shifts by the year
            Predicate shiftPredicate = criteriaBuilder.equal(criteriaBuilder.function("YEAR", Integer.class,
                    shifts.get("shiftDate")), year);
            return shiftPredicate;
        };
    }

    public static Specification<Employee> hasOffDatesInYear(int year) {
        return (root, query, criteriaBuilder) -> {
            // Join with offDates
            Join<Employee, PreferredOffDate> offDates = root.join("offDates");
            // Create a predicate for filtering off dates by the year
            Predicate offDatePredicate = criteriaBuilder.equal(criteriaBuilder.function("YEAR", Integer.class,
                    offDates.get("offDate")), year);
            return offDatePredicate;
        };
    }

    // Combine both specifications (shifts and offDates)
    public static Specification<Employee> hasShiftsAndOffDatesInYear(int year) {
        return hasShiftsInYear(year).and(hasOffDatesInYear(year));
    }
}
