package com.empmgmt.repository;

import com.empmgmt.model.LeaveRequest;
import com.empmgmt.model.LeaveStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);
    
    void deleteByEmployeeId(Long employeeId);

    List<LeaveRequest> findByStatus(LeaveStatus status);
    long countByEmployeeId(Long employeeId);

    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);
}
