package com.empmgmt.service.impl;

import com.empmgmt.dto.EmployeeDTO;
import com.empmgmt.dto.EmployeeSearchRequest;
import com.empmgmt.dto.PaginatedResponse;
import com.empmgmt.exception.ResourceNotFoundException;
import com.empmgmt.mapper.EmployeeMapper;
import com.empmgmt.model.Employee;
import com.empmgmt.model.EmployeeStatus;
import com.empmgmt.model.Role;
import com.empmgmt.repository.*;
import com.empmgmt.security.model.User;
import com.empmgmt.security.repository.UserRepository;
import com.empmgmt.security.service.CustomUserDetails;
import com.empmgmt.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repo;
    private final EmployeeMapper mapper;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final OnboardingFlowRepository onboardingFlowRepository;
    private final OnboardingTaskRepository onboardingTaskRepository;
    private final PayrollRepository payrollRepository;
    private final EmployeeKPIRepository employeeKPIRepository;
    private final PerformanceRepository performanceRepository;
    /* =========================================================
       SAFE SORT PARSER
       ========================================================= */
    private Sort getSafeSort(String sortValue) {
        if (sortValue == null || !sortValue.contains(",")) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        String[] arr = sortValue.split(",");
        String field = arr[0].trim();
        String direction = arr.length > 1 ? arr[1].trim() : "asc";

        return Sort.by(
                Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.ASC),
                field
        );
    }

    /* =========================================================
       CREATE
       ========================================================= */
    @Override
    public EmployeeDTO createEmployee(EmployeeDTO dto) {

        if (repo.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Employee employee = mapper.toEntity(dto);
        employee.setStatus(EmployeeStatus.ACTIVE);
        Employee savedEmployee = repo.save(employee);
        
        User user = new User();
        user.setUsername(savedEmployee.getEmail()); // login email
        user.setPassword(passwordEncoder.encode("1234")); // default password
        user.setRole(Role.ROLE_EMPLOYEE);
        user.setEmployee(savedEmployee);

        userRepository.save(user);
        
        return mapper.toDTO(savedEmployee);

       // return mapper.toDTO(repo.save(employee));
    }

    /* =========================================================
       UPDATE
       ========================================================= */
    @Override
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {

        Employee employee = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!employee.getEmail().equals(dto.getEmail())
                && repo.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPhone(dto.getPhone());
        employee.setDepartment(dto.getDepartment());
        employee.setPosition(dto.getPosition());
        employee.setSalary(dto.getSalary());

        Employee saved = repo.save(employee);

        // Synchronize corresponding User username if email changes
        userRepository.findByEmployee(saved).ifPresent(u -> {
            u.setUsername(dto.getEmail());
            userRepository.save(u);
        });

        return mapper.toDTO(saved);
    }

    /* =========================================================
       HARD DELETE
       ========================================================= */
    @Override
    @Transactional
    public void deleteEmployee(Long id) {

        Employee employee = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // 1. Delete user login credentials linked to this employee
        userRepository.findByEmployee(employee).ifPresent(u -> {
            userRepository.delete(u);
            userRepository.flush();
        });

        // 2. Delete child records referencing employeeId
        attendanceRepository.deleteByEmployeeId(id);
        leaveRepository.deleteByEmployeeId(id);
        leaveBalanceRepository.deleteByEmployeeId(id);
        onboardingTaskRepository.deleteByEmployeeId(id);
        onboardingFlowRepository.deleteByEmployeeId(id);
        payrollRepository.deleteByEmployeeId(id);
        employeeKPIRepository.deleteByEmployeeId(id);
        performanceRepository.deleteByEmployeeId(id);

        // 3. Delete the employee record itself
        repo.delete(employee);
    }

    /* =========================================================
       GET ONE
       ========================================================= */
    @Override
    public EmployeeDTO getEmployee(Long id) {

        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    /* =========================================================
       SEARCH (FINAL, SAFE, CORRECT)
       ========================================================= */
    @Override
    public PaginatedResponse<EmployeeDTO> searchEmployees(EmployeeSearchRequest req) {

        Pageable pageable = PageRequest.of(
                req.getPage(),
                req.getSize(),
                getSafeSort(req.getSort())
        );

        Page<Employee> page;
        EmployeeStatus status = req.getStatus() != null ? req.getStatus() : EmployeeStatus.ACTIVE;

        if (req.getSearch() != null && !req.getSearch().isBlank()) {

            page = repo.searchByStatus(
                    status,
                    req.getSearch(),
                    pageable
            );

        } else if (req.getDepartment() != null && !req.getDepartment().isBlank()) {

            page = repo.findByStatusAndDepartmentContainingIgnoreCase(
                    status,
                    req.getDepartment(),
                    pageable
            );

        } else {

            page = repo.findByStatus(
                    status,
                    pageable
            );
        }

        return PaginatedResponse.<EmployeeDTO>builder()
                .content(page.getContent().stream().map(mapper::toDTO).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /* =========================================================
       GET ALL
       ========================================================= */
    @Override
    public List<EmployeeDTO> getAllEmployees() {

        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /* =========================================================
       EMAIL → EMPLOYEE
       ========================================================= */
    @Override
    public Employee getByEmail(String email) {

        return repo.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found for email: " + email
                        )
                );
    }

    /* =========================================================
       AUTH → EMPLOYEE ID
       ========================================================= */
    @Override
    public Long getEmployeeIdFromAuth() {

        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof CustomUserDetails user) {
            return user.getEmployeeId();
        }

        throw new IllegalStateException("User not authenticated");
    }
}
