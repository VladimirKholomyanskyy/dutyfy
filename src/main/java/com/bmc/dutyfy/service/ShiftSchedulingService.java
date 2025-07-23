package com.bmc.dutyfy.service;

import com.bmc.dutyfy.model.*;
import com.bmc.dutyfy.repository.AdminConstraintRepository;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.repository.PreferredOffDateRepository;
import com.bmc.dutyfy.repository.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShiftSchedulingService {

    // Define your company holidays here or load from database
    private final List<LocalDate> holidays = Arrays.asList(
            LocalDate.of(2025, 1, 1),   // New Year
            LocalDate.of(2025, 7, 4),   // Independence Day
            LocalDate.of(2025, 12, 25)  // Christmas
    );
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private AdminConstraintRepository adminConstraintRepository;
    @Autowired
    private PreferredOffDateRepository preferredOffDateRepository;
    @Autowired
    private EmailService emailService;
    @Value("${dutyfy.schedule.notification-days-before:7}")
    private int notificationDaysBefore;
    @Value("${dutyfy.schedule.max-preferred-off-days:5}")
    private int maxPreferredOffDays;

    public DutyScheduler.SchedulingResult createYearlySchedule(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // Get active employees
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(Employee::isActive)
                .collect(Collectors.toList());

        if (activeEmployees.isEmpty()) {
            List<String> warnings = Arrays.asList("No active employees found for scheduling");
            emailService.sendSchedulingFailureEmail(warnings, year);
            return new DutyScheduler.SchedulingResult(false, Arrays.asList(), warnings);
        }

        // Get admin constraints for the year
        List<AdminConstraint> adminConstraints = adminConstraintRepository.findByYear(year);

        // Create schedule
        DutyScheduler.SchedulingResult result = DutyScheduler.scheduleDuties(
                activeEmployees, startDate, endDate, adminConstraints, holidays);

        if (result.isSuccess()) {
            // Clear existing shifts for the year
            clearExistingShifts(year);

            // Save new shifts
            shiftRepository.saveAll(result.getShifts());

            // Update previous year shift counts
            updatePreviousYearShiftCounts(year);

            System.out.println("✅ Successfully created schedule for " + year);
        } else {
            emailService.sendSchedulingFailureEmail(result.getWarnings(), year);
        }

        return result;
    }

    private void clearExistingShifts(int year) {
        List<Shift> existingShifts = shiftRepository.findAll().stream()
                .filter(shift -> shift.getShiftDate().getYear() == year)
                .collect(Collectors.toList());

        if (!existingShifts.isEmpty()) {
            shiftRepository.deleteAll(existingShifts);
            System.out.println("Cleared " + existingShifts.size() + " existing shifts for " + year);
        }
    }

    private void updatePreviousYearShiftCounts(int currentYear) {
        int previousYear = currentYear - 1;
        List<Employee> employees = employeeRepository.findAll();

        for (Employee employee : employees) {
            long previousYearCount = employee.getShifts().stream()
                    .filter(shift -> shift.getShiftDate().getYear() == previousYear)
                    .count();

            employee.setPreviousYearShifts((int) previousYearCount);
            employeeRepository.save(employee);
        }
    }

    // Scheduled task to send reminders (runs daily at 9 AM)
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendOffDateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate scheduleCreationDate = getNextScheduleCreationDate();

        if (scheduleCreationDate != null &&
                today.equals(scheduleCreationDate.minusDays(notificationDaysBefore))) {

            List<Employee> activeEmployees = employeeRepository.findAll().stream()
                    .filter(Employee::isActive)
                    .collect(Collectors.toList());

            for (Employee employee : activeEmployees) {
                emailService.sendOffDateReminderEmail(
                        employee.getEmail(),
                        employee.getName(),
                        scheduleCreationDate.getYear()
                );
            }

            System.out.println("Sent off-date reminder emails to " + activeEmployees.size() + " employees");
        }
    }

    private LocalDate getNextScheduleCreationDate() {
        // This is a simple implementation - you might want to store this in configuration
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() <= 11) {
            return LocalDate.of(now.getYear(), 12, 1); // December 1st for next year
        } else {
            return LocalDate.of(now.getYear() + 1, 12, 1);
        }
    }

    public boolean validatePreferredOffDates(Employee employee, List<LocalDate> offDates) {
        if (offDates.size() > maxPreferredOffDays) {
            return false;
        }

        // Additional validation logic can be added here
        return true;
    }

    public void savePreferredOffDates(Employee employee, List<LocalDate> offDates, int year) {
        if (!validatePreferredOffDates(employee, offDates)) {
            throw new IllegalArgumentException("Too many preferred off dates. Maximum allowed: " + maxPreferredOffDays);
        }

        // Clear existing off dates for the year
        List<PreferredOffDate> existing = employee.getOffDates().stream()
                .filter(offDate -> offDate.getOffDate().getYear() == year)
                .collect(Collectors.toList());

        preferredOffDateRepository.deleteAll(existing);

        // Save new off dates
        List<PreferredOffDate> newOffDates = offDates.stream()
                .map(date -> new PreferredOffDate(employee, date))
                .collect(Collectors.toList());

        preferredOffDateRepository.saveAll(newOffDates);
    }

    public List<Shift> getShiftsForYear(int year) {
        return shiftRepository.findAll().stream()
                .filter(shift -> shift.getShiftDate().getYear() == year)
                .collect(Collectors.toList());
    }

    public List<Shift> getShiftsForEmployee(Employee employee, int year) {
        return employee.getShifts().stream()
                .filter(shift -> shift.getShiftDate().getYear() == year)
                .collect(Collectors.toList());
    }
}