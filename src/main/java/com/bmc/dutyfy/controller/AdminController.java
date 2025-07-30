package com.bmc.dutyfy.controller;

import com.bmc.dutyfy.model.DutyScheduler;
import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.PreferredOffDate;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.service.ShiftSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ShiftSchedulingService schedulingService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/schedule")
    public String scheduleManagement(Model model) {
        int currentYear = LocalDate.now().getYear();
        int nextYear = currentYear + 1;

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("nextYear", nextYear);

        return "admin/schedule-management";
    }

    @PostMapping("/schedule/create/{year}")
    public String createSchedule(@PathVariable int year, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🚀 Starting schedule creation for year: " + year);

            DutyScheduler.SchedulingResult result = schedulingService.createYearlySchedule(year);

            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Schedule for " + year + " created successfully with " + result.getShifts().size() + " shifts" +
                                ".");

                if (!result.getWarnings().isEmpty()) {
                    redirectAttributes.addFlashAttribute("warnings", result.getWarnings());
                }

                System.out.println("✅ Schedule creation completed successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Failed to create schedule for " + year + ". Check the warnings for details.");
                redirectAttributes.addFlashAttribute("warnings", result.getWarnings());

                System.out.println("❌ Schedule creation failed");
            }
        } catch (Exception e) {
            System.err.println("💥 Exception during schedule creation: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating schedule: " + e.getMessage());
        }

        return "redirect:/admin/schedule";
    }

    @GetMapping("/employees")
    public String employeeManagement() {
        return "admin/employee-management";
    }

    @GetMapping("/off-dates")
    public String viewOffDates(Model model) {
        int nextYear = LocalDate.now().getYear() + 1;

        List<Employee> employees = employeeRepository.findAll();

        // Group off dates by employee
        Map<Employee, List<PreferredOffDate>> offDatesByEmployee = employees.stream()
                .collect(Collectors.toMap(
                        employee -> employee,
                        employee -> employee.getOffDates().stream()
                                .filter(offDate -> offDate.getOffDate().getYear() == nextYear)
                                .sorted((a, b) -> a.getOffDate().compareTo(b.getOffDate()))
                                .collect(Collectors.toList())
                ));

        model.addAttribute("year", nextYear);
        model.addAttribute("offDatesByEmployee", offDatesByEmployee);
        model.addAttribute("submissionDeadline", LocalDate.of(LocalDate.now().getYear(), 11, 24));

        return "admin/off-dates-summary";
    }

    @GetMapping("/constraints")
    public String constraintManagement() {
        return "admin/constraint-management";
    }
}