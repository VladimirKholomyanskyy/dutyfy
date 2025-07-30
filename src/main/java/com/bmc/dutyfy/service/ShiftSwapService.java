package com.bmc.dutyfy.service;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.Shift;
import com.bmc.dutyfy.model.ShiftSwapRequest;
import com.bmc.dutyfy.model.SwapStatus;
import com.bmc.dutyfy.repository.ShiftRepository;
import com.bmc.dutyfy.repository.ShiftSwapRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShiftSwapService {

    @Autowired
    private ShiftSwapRequestRepository swapRequestRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private EmailService emailService;

    public ShiftSwapRequest createSwapRequest(Employee requester, Employee targetEmployee,
                                              Shift shift, String reason) {
        // Validate that the requester owns the shift
        if (!shift.getEmployee().equals(requester)) {
            throw new IllegalArgumentException("You can only request swaps for your own shifts");
        }

        // Check if there's already a pending request for this shift
        List<ShiftSwapRequest> existingRequests = swapRequestRepository.findAll().stream()
                .filter(req -> req.getShift().equals(shift) && req.getStatus() == SwapStatus.PENDING)
                .toList();

        if (!existingRequests.isEmpty()) {
            throw new IllegalArgumentException("There is already a pending swap request for this shift");
        }

        // Create and save the request
        ShiftSwapRequest swapRequest = new ShiftSwapRequest(requester, targetEmployee, shift, reason);
        swapRequest = swapRequestRepository.save(swapRequest);

        // Send notification email
        emailService.sendSwapRequestEmail(
                targetEmployee.getEmail(),
                targetEmployee.getName(),
                requester.getName(),
                shift.getShiftDate().toString(),
                reason
        );

        return swapRequest;
    }

    public void approveSwapRequest(Long requestId, Employee approver) {
        Optional<ShiftSwapRequest> optionalRequest = swapRequestRepository.findById(requestId);
        if (optionalRequest.isEmpty()) {
            throw new IllegalArgumentException("Swap request not found");
        }

        ShiftSwapRequest request = optionalRequest.get();

        // Validate that the approver is the target employee
        if (!request.getTargetEmployee().equals(approver)) {
            throw new IllegalArgumentException("Only the target employee can approve this request");
        }

        if (request.getStatus() != SwapStatus.PENDING) {
            throw new IllegalArgumentException("This request has already been processed");
        }

        // Find the target employee's shift on the same date
        Optional<Shift> targetShift = shiftRepository.findAll().stream()
                .filter(s -> s.getEmployee().equals(request.getTargetEmployee()) &&
                        s.getShiftDate().equals(request.getShift().getShiftDate()))
                .findFirst();

        if (targetShift.isEmpty()) {
            throw new IllegalArgumentException("Target employee doesn't have a shift on the same date");
        }

        // Perform the swap
        Shift requesterShift = request.getShift();
        Shift targetEmployeeShift = targetShift.get();

        Employee tempEmployee = requesterShift.getEmployee();
        requesterShift.setEmployee(targetEmployeeShift.getEmployee());
        targetEmployeeShift.setEmployee(tempEmployee);

        shiftRepository.save(requesterShift);
        shiftRepository.save(targetEmployeeShift);

        // Update request status
        request.setStatus(SwapStatus.APPROVED);
        request.setResponseDate(LocalDateTime.now());
        swapRequestRepository.save(request);

        System.out.println("✅ Shift swap approved: " + request.getRequester().getName() +
                " ↔ " + request.getTargetEmployee().getName() +
                " on " + request.getShift().getShiftDate());
    }

    public void rejectSwapRequest(Long requestId, Employee rejecter) {
        Optional<ShiftSwapRequest> optionalRequest = swapRequestRepository.findById(requestId);
        if (optionalRequest.isEmpty()) {
            throw new IllegalArgumentException("Swap request not found");
        }

        ShiftSwapRequest request = optionalRequest.get();

        // Validate that the rejecter is the target employee
        if (!request.getTargetEmployee().equals(rejecter)) {
            throw new IllegalArgumentException("Only the target employee can reject this request");
        }

        if (request.getStatus() != SwapStatus.PENDING) {
            throw new IllegalArgumentException("This request has already been processed");
        }

        // Update request status
        request.setStatus(SwapStatus.REJECTED);
        request.setResponseDate(LocalDateTime.now());
        swapRequestRepository.save(request);

        System.out.println("❌ Shift swap rejected: " + request.getRequester().getName() +
                " → " + request.getTargetEmployee().getName() +
                " on " + request.getShift().getShiftDate());
    }

    public List<ShiftSwapRequest> getPendingRequestsForEmployee(Employee employee) {
        return swapRequestRepository.findByTargetEmployeeAndStatusOrderByRequestDateDesc(employee, SwapStatus.PENDING);
    }

    public List<ShiftSwapRequest> getRequestsByEmployee(Employee employee) {
        return swapRequestRepository.findByRequesterOrderByRequestDateDesc(employee);
    }

    public List<ShiftSwapRequest> getAllRequestsForEmployee(Employee employee) {
        // Get both requests made by employee and requests received by employee
        List<ShiftSwapRequest> sentRequests = swapRequestRepository.findByRequesterOrderByRequestDateDesc(employee);
        List<ShiftSwapRequest> receivedRequests =
                swapRequestRepository.findByTargetEmployeeOrderByRequestDateDesc(employee);

        // Combine and sort by request date
        List<ShiftSwapRequest> allRequests = new ArrayList<>();
        allRequests.addAll(sentRequests);
        allRequests.addAll(receivedRequests);

        return allRequests.stream()
                .sorted((a, b) -> b.getRequestDate().compareTo(a.getRequestDate()))
                .collect(Collectors.toList());
    }

    public List<ShiftSwapRequest> getAllPendingRequests() {
        return swapRequestRepository.findByStatusOrderByRequestDateDesc(SwapStatus.PENDING);
    }
}