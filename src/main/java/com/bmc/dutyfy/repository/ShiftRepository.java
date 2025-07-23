package com.bmc.dutyfy.repository;

import com.bmc.dutyfy.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
}
