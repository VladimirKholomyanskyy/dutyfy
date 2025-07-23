package com.bmc.dutyfy.controller;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.Shift;
import com.bmc.dutyfy.model.ShiftSwapRequest;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.service.ShiftSchedulingService;
import com.bmc.dutyfy.service.ShiftSwapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ShiftSchedulingService schedulingService;

    @Autowired
    private ShiftSwapService swapService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            return "admin-dashboard";
        } else {
            // Find employee by email (username in this case is email)
            Optional<Employee> employee = employeeRepository.findByEmail(username);

            if (employee.isPresent()) {
                int currentYear = LocalDate.now().getYear();
                List<Shift> myShifts = schedulingService.getShiftsForEmployee(employee.get(), currentYear);
                List<ShiftSwapRequest> pendingRequests = swapService.getPendingRequestsForEmployee(employee.get());

                model.addAttribute("employee", employee.get());
                model.addAttribute("myShifts", myShifts);
                model.addAttribute("pendingRequests", pendingRequests);
                model.addAttribute("currentYear", currentYear);
            } else {
                // If employee not found in database, show error
                model.addAttribute("error", "Employee profile not found. Please contact administrator.");
            }

            return "employee-dashboard";
        }
    }

    @GetMapping("/shifts/{year}")
    public String viewShifts(@PathVariable int year, Model model, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        List<Shift> shifts = schedulingService.getShiftsForYear(year);
        model.addAttribute("shifts", shifts);
        model.addAttribute("year", year);
        model.addAttribute("isAdmin", isAdmin);

        return "shifts-calendar";
    }
}