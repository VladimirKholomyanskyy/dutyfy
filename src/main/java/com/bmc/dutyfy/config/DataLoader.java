package com.bmc.dutyfy.config;

import com.bmc.dutyfy.model.AdminConstraint;
import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.PreferredOffDate;
import com.bmc.dutyfy.model.UserRole;
import com.bmc.dutyfy.repository.AdminConstraintRepository;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.repository.PreferredOffDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PreferredOffDateRepository preferredOffDateRepository;

    @Autowired
    private AdminConstraintRepository adminConstraintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (employeeRepository.count() == 0) {
            loadSampleData();
        }
    }

    private void loadSampleData() {
        // Create sample employees
        Employee admin = new Employee("Admin User", "admin@company.com",
                passwordEncoder.encode("admin123"), UserRole.ADMIN, true);
        admin.setPreviousYearShifts(45);

        Employee john = new Employee("John Doe", "john.doe@company.com",
                passwordEncoder.encode("password"), UserRole.EMPLOYEE, true);
        john.setPreviousYearShifts(52);

        Employee jane = new Employee("Jane Smith", "jane.smith@company.com",
                passwordEncoder.encode("password"), UserRole.EMPLOYEE, true);
        jane.setPreviousYearShifts(48);

        Employee mike = new Employee("Mike Johnson", "mike.johnson@company.com",
                passwordEncoder.encode("password"), UserRole.EMPLOYEE, true);
        mike.setPreviousYearShifts(50);

        Employee sarah = new Employee("Sarah Wilson", "sarah.wilson@company.com",
                passwordEncoder.encode("password"), UserRole.EMPLOYEE, true);
        sarah.setPreviousYearShifts(46);

        // Save employees
        employeeRepository.saveAll(Arrays.asList(admin, john, jane, mike, sarah));

        // Create sample preferred off dates for 2025
        int year = LocalDate.now().getYear();
        if (year <= 2025) {
            // John's preferred off dates
            preferredOffDateRepository.saveAll(Arrays.asList(
                    new PreferredOffDate(john, LocalDate.of(2025, 3, 15)),
                    new PreferredOffDate(john, LocalDate.of(2025, 6, 20)),
                    new PreferredOffDate(john, LocalDate.of(2025, 8, 10)),
                    new PreferredOffDate(john, LocalDate.of(2025, 11, 28))
            ));

            // Jane's preferred off dates
            preferredOffDateRepository.saveAll(Arrays.asList(
                    new PreferredOffDate(jane, LocalDate.of(2025, 2, 14)),
                    new PreferredOffDate(jane, LocalDate.of(2025, 5, 25)),
                    new PreferredOffDate(jane, LocalDate.of(2025, 9, 5)),
                    new PreferredOffDate(jane, LocalDate.of(2025, 12, 20))
            ));

            // Sample admin constraints (religious holidays)
            adminConstraintRepository.saveAll(Arrays.asList(
                    new AdminConstraint(john, LocalDate.of(2025, 4, 18), "Good Friday", false),
                    new AdminConstraint(jane, LocalDate.of(2025, 9, 15), "Rosh Hashanah", false),
                    new AdminConstraint(mike, LocalDate.of(2025, 12, 25), "Christmas", false)
            ));
        }

        System.out.println("✅ Sample data loaded successfully!");
        System.out.println("👤 Admin login: admin@company.com / admin123");
        System.out.println("👤 Employee login: john.doe@company.com / password");
        System.out.println("👤 Employee login: jane.smith@company.com / password");
    }
}