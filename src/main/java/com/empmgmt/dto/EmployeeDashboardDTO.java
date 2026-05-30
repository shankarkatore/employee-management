package com.empmgmt.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardDTO {

    private Long employeeId;

    private long myLeaves;

    private long approvedLeaves;

    private long pendingLeaves;

    private long rejectedLeaves;

    private long activeOnboarding;

    private java.util.List<String> months;

    private java.util.List<Long> performanceScores;
}