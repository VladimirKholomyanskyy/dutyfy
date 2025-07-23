package com.bmc.dutyfy.controller;

import com.bmc.dutyfy.model.DutyScheduler;
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

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ShiftSchedulingService schedulingService;

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
            DutyScheduler.SchedulingResult result = schedulingService.createYearlySchedule(year);

            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Schedule for " + year + " created successfully with " + result.getShifts().size() + " shifts" +
                                ".");

                if (!result.getWarnings().isEmpty()) {
                    redirectAttributes.addFlashAttribute("warnings", result.getWarnings());
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Failed to create schedule for " + year + ". Check the warnings for details.");
                redirectAttributes.addFlashAttribute("warnings", result.getWarnings());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating schedule: " + e.getMessage());
        }

        return "redirect:/admin/schedule";
    }

    @GetMapping("/employees")
    public String employeeManagement() {
        return "admin/employee-management";
    }

    @GetMapping("/constraints")
    public String constraintManagement() {
        return "admin/constraint-management";
    }
}