package com.bmc.dutyfy.controller;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.PreferredOffDate;
import com.bmc.dutyfy.model.ShiftSwapRequest;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.service.ShiftSchedulingService;
import com.bmc.dutyfy.service.ShiftSwapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    ShiftSwapService swapService;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ShiftSchedulingService schedulingService;

    @GetMapping("/off-dates")
    public String showOffDatesForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<Employee> employee = employeeRepository.findByEmail(username);

        if (employee.isEmpty()) {
            model.addAttribute("error", "Employee not found");
            return "redirect:/dashboard";
        }

        int currentYear = LocalDate.now().getYear();
        int nextYear = currentYear + 1;

        // Get existing off dates for next year
        List<PreferredOffDate> existingOffDates = employee.get().getOffDates().stream()
                .filter(offDate -> offDate.getOffDate().getYear() == nextYear)
                .sorted((a, b) -> a.getOffDate().compareTo(b.getOffDate()))
                .toList();

        // Calculate deadline for submissions
        LocalDate submissionDeadline = LocalDate.of(currentYear, 11, 24); // November 24th
        boolean isDeadlinePassed = LocalDate.now().isAfter(submissionDeadline);

        model.addAttribute("employee", employee.get());
        model.addAttribute("year", nextYear);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("existingOffDates", existingOffDates);
        model.addAttribute("maxOffDays", 5);
        model.addAttribute("submissionDeadline", submissionDeadline);
        model.addAttribute("isDeadlinePassed", isDeadlinePassed);

        return "employee/off-dates";
    }

    @PostMapping("/off-dates")
    public String submitOffDates(@RequestParam("offDates") List<String> offDateStrings,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Employee> employee = employeeRepository.findByEmail(username);

        if (employee.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Employee not found");
            return "redirect:/dashboard";
        }

        try {
            // Check deadline
            int currentYear = LocalDate.now().getYear();
            LocalDate submissionDeadline = LocalDate.of(currentYear, 11, 24);
            if (LocalDate.now().isAfter(submissionDeadline)) {
                redirectAttributes.addFlashAttribute("error",
                        "Submission deadline has passed. Off-date requests for " + (currentYear + 1) + " are no " +
                                "longer accepted.");
                return "redirect:/employee/off-dates";
            }

            List<LocalDate> offDates = new ArrayList<>();
            int nextYear = currentYear + 1;

            for (String dateStr : offDateStrings) {
                if (!dateStr.trim().isEmpty()) {
                    LocalDate date = LocalDate.parse(dateStr.trim());

                    // Validate date is in the correct year
                    if (date.getYear() != nextYear) {
                        redirectAttributes.addFlashAttribute("error",
                                "All dates must be in " + nextYear + ". Invalid date: " + date);
                        return "redirect:/employee/off-dates";
                    }

                    // Check for duplicates
                    if (offDates.contains(date)) {
                        redirectAttributes.addFlashAttribute("error",
                                "Duplicate date found: " + date + ". Please remove duplicates.");
                        return "redirect:/employee/off-dates";
                    }

                    offDates.add(date);
                }
            }

            // Validate number of dates
            if (offDates.size() > 5) {
                redirectAttributes.addFlashAttribute("error",
                        "Too many off dates. Maximum allowed: 5. You submitted: " + offDates.size());
                return "redirect:/employee/off-dates";
            }

            schedulingService.savePreferredOffDates(employee.get(), offDates, nextYear);

            String message = offDates.isEmpty() ?
                    "All preferred off dates cleared for " + nextYear :
                    "Successfully saved " + offDates.size() + " preferred off dates for " + nextYear;

            redirectAttributes.addFlashAttribute("success", message);

            System.out.println("✅ " + employee.get().getName() + " submitted " + offDates.size() + " off dates for " + nextYear);

        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid date format. Please use the date picker or YYYY-MM-DD format.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            System.err.println("Error saving off dates for " + username + ": " + e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while saving your off dates. Please try again.");
        }

        return "redirect:/employee/off-dates";
    }

    @GetMapping("/swap-requests")
    public String showSwapRequests(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<Employee> employee = employeeRepository.findByEmail(username);

        if (employee.isEmpty()) {
            model.addAttribute("error", "Employee not found");
            return "redirect:/dashboard";
        }

        // Get requests I've made

        List<ShiftSwapRequest> myRequests = swapService.getRequestsByEmployee(employee.get());

        // Get requests sent to me
        List<ShiftSwapRequest> requestsForMe = swapService.getPendingRequestsForEmployee(employee.get());

        // Get all requests (sent and received) for history
        List<ShiftSwapRequest> allMyRequests = swapService.getAllRequestsForEmployee(employee.get());

        model.addAttribute("employee", employee.get());
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("requestsForMe", requestsForMe);
        model.addAttribute("allRequests", allMyRequests);

        return "employee/swap-requests";
    }
}