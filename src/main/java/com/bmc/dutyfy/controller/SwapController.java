package com.bmc.dutyfy.controller;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.Shift;
import com.bmc.dutyfy.model.ShiftSwapRequest;
import com.bmc.dutyfy.repository.EmployeeRepository;
import com.bmc.dutyfy.repository.ShiftRepository;
import com.bmc.dutyfy.service.ShiftSchedulingService;
import com.bmc.dutyfy.service.ShiftSwapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/swap")
public class SwapController {

    @Autowired
    private ShiftSwapService swapService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftSchedulingService schedulingService;

    @GetMapping("/request")
    public String showSwapRequestForm(@RequestParam(required = false) Long shiftId,
                                      Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<Employee> currentEmployee = employeeRepository.findByEmail(username);

        if (currentEmployee.isEmpty()) {
            model.addAttribute("error", "Employee not found");
            return "redirect:/dashboard";
        }

        int currentYear = LocalDate.now().getYear();

        // Get current employee's shifts for this year and next year
        List<Shift> myShifts = schedulingService.getShiftsForEmployee(currentEmployee.get(), currentYear);
        myShifts.addAll(schedulingService.getShiftsForEmployee(currentEmployee.get(), currentYear + 1));

        // Filter out past shifts (can't swap past shifts)
        List<Shift> availableShifts = myShifts.stream()
                .filter(shift -> shift.getShiftDate().isAfter(LocalDate.now()))
                .sorted((a, b) -> a.getShiftDate().compareTo(b.getShiftDate()))
                .toList();

        // Get all other active employees
        List<Employee> otherEmployees = employeeRepository.findByActiveTrue().stream()
                .filter(emp -> !emp.equals(currentEmployee.get()))
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();

        // If a specific shift is requested, find it
        Shift selectedShift = null;
        if (shiftId != null) {
            selectedShift = availableShifts.stream()
                    .filter(shift -> shift.getId().equals(shiftId))
                    .findFirst()
                    .orElse(null);
        }

        model.addAttribute("employee", currentEmployee.get());
        model.addAttribute("myShifts", availableShifts);
        model.addAttribute("otherEmployees", otherEmployees);
        model.addAttribute("selectedShift", selectedShift);

        return "employee/swap-request";
    }

    @PostMapping("/request")
    public String submitSwapRequest(@RequestParam Long shiftId,
                                    @RequestParam Long targetEmployeeId,
                                    @RequestParam String reason,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔄 Swap request received:");
            System.out.println("   Shift ID: " + shiftId);
            System.out.println("   Target Employee ID: " + targetEmployeeId);
            System.out.println("   Reason: " + reason);

            String username = authentication.getName();
            System.out.println("   Requester: " + username);

            Optional<Employee> requester = employeeRepository.findByEmail(username);
            Optional<Employee> targetEmployee = employeeRepository.findById(targetEmployeeId);
            Optional<Shift> shift = shiftRepository.findById(shiftId);

            if (requester.isEmpty()) {
                System.err.println("❌ Requester not found: " + username);
                redirectAttributes.addFlashAttribute("error", "Requester not found");
                return "redirect:/employee/swap-requests";
            }

            if (targetEmployee.isEmpty()) {
                System.err.println("❌ Target employee not found: " + targetEmployeeId);
                redirectAttributes.addFlashAttribute("error", "Target employee not found");
                return "redirect:/employee/swap-requests";
            }

            if (shift.isEmpty()) {
                System.err.println("❌ Shift not found: " + shiftId);
                redirectAttributes.addFlashAttribute("error", "Shift not found");
                return "redirect:/employee/swap-requests";
            }

            System.out.println("✅ All entities found:");
            System.out.println("   Requester: " + requester.get().getName());
            System.out.println("   Target: " + targetEmployee.get().getName());
            System.out.println("   Shift Date: " + shift.get().getShiftDate());

            // Validate that the shift belongs to the requester
            if (!shift.get().getEmployee().equals(requester.get())) {
                System.err.println("❌ Shift ownership validation failed");
                System.err.println("   Shift owner: " + shift.get().getEmployee().getName());
                System.err.println("   Requester: " + requester.get().getName());
                redirectAttributes.addFlashAttribute("error", "You can only request swaps for your own shifts");
                return "redirect:/employee/swap-requests";
            }

            // Check if target employee has a shift on the same date
            LocalDate shiftDate = shift.get().getShiftDate();
            System.out.println("🔍 Checking if target has shift on: " + shiftDate);

            List<Shift> targetShifts = schedulingService.getShiftsForEmployee(targetEmployee.get(),
                    shiftDate.getYear());
            System.out.println("   Target has " + targetShifts.size() + " shifts in " + shiftDate.getYear());

            boolean targetHasShiftOnDate = targetShifts.stream()
                    .anyMatch(s -> s.getShiftDate().equals(shiftDate));

            System.out.println("   Target has shift on " + shiftDate + ": " + targetHasShiftOnDate);

            if (!targetHasShiftOnDate) {
                System.err.println("❌ Target employee doesn't have shift on same date");
                redirectAttributes.addFlashAttribute("error",
                        targetEmployee.get().getName() + " doesn't have a shift on " + shiftDate + " to swap with");
                return "redirect:/swap/request?shiftId=" + shiftId;
            }

            System.out.println("🚀 Creating swap request...");
            ShiftSwapRequest swapRequest = swapService.createSwapRequest(
                    requester.get(), targetEmployee.get(), shift.get(), reason);

            System.out.println("✅ Swap request created successfully: ID = " + swapRequest.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Swap request sent to " + targetEmployee.get().getName() + " for " + shiftDate);

        } catch (Exception e) {
            System.err.println("💥 Error creating swap request: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/employee/swap-requests";
    }

    @PostMapping("/approve")
    public ResponseEntity<String> approveSwap(@RequestParam Long requestId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<Employee> employee = employeeRepository.findByEmail(username);

            if (employee.isEmpty()) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            swapService.approveSwapRequest(requestId, employee.get());
            return ResponseEntity.ok("Swap request approved successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<String> rejectSwap(@RequestParam Long requestId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<Employee> employee = employeeRepository.findByEmail(username);

            if (employee.isEmpty()) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            swapService.rejectSwapRequest(requestId, employee.get());
            return ResponseEntity.ok("Swap request rejected");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}