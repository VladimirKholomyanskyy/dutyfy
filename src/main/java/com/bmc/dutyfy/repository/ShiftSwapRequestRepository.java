package com.bmc.dutyfy.repository;

import com.bmc.dutyfy.model.Employee;
import com.bmc.dutyfy.model.ShiftSwapRequest;
import com.bmc.dutyfy.model.SwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, Long> {

    List<ShiftSwapRequest> findByRequesterOrderByRequestDateDesc(Employee requester);

    List<ShiftSwapRequest> findByTargetEmployeeOrderByRequestDateDesc(Employee targetEmployee);

    List<ShiftSwapRequest> findByStatusOrderByRequestDateDesc(SwapStatus status);

    List<ShiftSwapRequest> findByTargetEmployeeAndStatusOrderByRequestDateDesc(Employee targetEmployee,
                                                                               SwapStatus status);
}