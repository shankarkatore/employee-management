package com.empmgmt.service.impl;

import com.empmgmt.dto.DashboardDTO;
import com.empmgmt.dto.OnboardingSummaryDTO;
import com.empmgmt.model.Employee;
import com.empmgmt.model.EmployeeStatus;
import com.empmgmt.model.LeaveStatus;
import com.empmgmt.model.OnboardingStatus;
import com.empmgmt.model.OnboardingTask;
import com.empmgmt.repository.*;
import com.empmgmt.service.DashboardSummaryService;
import com.empmgmt.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

import static com.empmgmt.model.LeaveStatus.PENDING;

@Service
@RequiredArgsConstructor
public class DashboardSummaryServiceImpl implements DashboardSummaryService {

    private final EmployeeRepository employeeRepo;
    private final DepartmentRepository departmentRepo;
    private final LeaveRepository leaveRepo;
    private final EmployeeService employeeService;
    private final OnboardingFlowRepository flowRepo;
   
  
    private final OnboardingTaskRepository onboardingTaskRepo;

    @Override
    public DashboardDTO getDashboardStats() {
    	

        DashboardDTO dto = new DashboardDTO();
        

        // -----------------------------------------
        //  BASIC KPI
        // -----------------------------------------
      //  dto.setTotalEmployees(employeeRepo.count());
      dto.setOnboardingActive(
    (int) flowRepo.findAll().stream()
        .filter(f -> !f.isCompleted())
        .count()
);
        dto.setPendingLeaves(leaveRepo.findByStatus(PENDING).size());
        
       
        Long empId = null;
        try {
            empId = employeeService.getEmployeeIdFromAuth();
        } catch (Exception e) {
            // Not authenticated or no employee linked (e.g. Admin)
        }

        if (empId != null) {
            dto.setEmployeeId(empId);
            dto.setMyLeaves(leaveRepo.countByEmployeeId(empId));
            dto.setMyPendingLeaves(
                leaveRepo.countByEmployeeIdAndStatus(empId, LeaveStatus.PENDING)
            );
            dto.setMyApprovedLeaves(
                leaveRepo.countByEmployeeIdAndStatus(empId, LeaveStatus.APPROVED)
            );
            dto.setMyRejectedLeaves(
                leaveRepo.countByEmployeeIdAndStatus(empId, LeaveStatus.REJECTED)
            );
            dto.setOnboardingStatus(
                flowRepo.findByEmployeeId(empId)
                    .map(f -> {
                        if (f.getStatus() == null) return "Not Started";
                        return switch (f.getStatus()) {
                            case IN_PROGRESS -> "In Progress";
                            case COMPLETED -> "Completed";
                            default -> "Not Started";
                        };
                    })
                    .orElse("Not Started")
            );
        } else {
            dto.setEmployeeId(null);
            dto.setMyLeaves(0);
            dto.setMyPendingLeaves(0);
            dto.setMyApprovedLeaves(0);
            dto.setMyRejectedLeaves(0);
            dto.setOnboardingStatus("N/A");
        }

        dto.setPerformanceMonths(
        	    List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun")
        	);

        	dto.setPerformanceScores(
        	    List.of(72, 78, 81, 85, 89, 94)
        	);
        // -----------------------------------------
        // PIPELINE COUNTS
        // -----------------------------------------
      

        // -----------------------------------------
        // EMPLOYEE GROWTH (LAST 12 MONTHS)
        // -----------------------------------------
        Map<String, Long> growth = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            LocalDate targetEnd = month.withDayOfMonth(month.lengthOfMonth());
            long count = employeeRepo.findAll().stream()
                    .filter(e -> {
                        if (e.getCreatedAt() == null) return false;
                        LocalDate empDate = e.getCreatedAt().toLocalDate();
                        return !empDate.isAfter(targetEnd);
                    })
                    .count();

            growth.put(label, count);
        }

        dto.setMonths(new ArrayList<>(growth.keySet()));
        dto.setEmployeeCounts(new ArrayList<>(growth.values()));

        // -----------------------------------------
        // LEAVE BREAKDOWN (real data)
        // -----------------------------------------
        dto.setLeavesApproved(
        	    leaveRepo.findByStatus(LeaveStatus.APPROVED).size()
        	);

        	dto.setLeavesPending(
        	    leaveRepo.findByStatus(LeaveStatus.PENDING).size()
        	);

        	dto.setLeavesRejected(
        	    leaveRepo.findByStatus(LeaveStatus.REJECTED).size()
        	);
        	dto.setTotalEmployees(
        		    employeeRepo.countByStatus(EmployeeStatus.ACTIVE)
        		);

        // -----------------------------------------
        // ONBOARDING SUMMARY (real data)
        // -----------------------------------------
        	dto.setOnboardingList(
        		    flowRepo.findAll().stream()
        		        .map(f -> {
                            Optional<Employee> empOpt = employeeRepo.findById(f.getEmployeeId());
                            String name = empOpt.map(e -> e.getFirstName() + " " + e.getLastName()).orElse("Employee " + f.getEmployeeId());
                            String email = empOpt.map(Employee::getEmail).orElse("");
                            return new OnboardingSummaryDTO(
                                f.getEmployeeId(),
                                name,
                                email,
                                f.getProgress()
                            );
                        })
        		        .toList()
        		);
        return dto;
    }
}
