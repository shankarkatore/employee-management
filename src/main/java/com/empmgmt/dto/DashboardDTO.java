package com.empmgmt.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    // KPI Cards
    private long totalEmployees;
    private long onboardingActive;
    private long pendingLeaves;
    private long openJobs;

    private Long employeeId;

    private long myLeaves;

    private long myPendingLeaves;

    private long myApprovedLeaves;

    private long myRejectedLeaves;

    private String onboardingStatus;

    // Employee Growth Chart
    private List<String> months;
    private List<Long> employeeCounts;
    private List<String> performanceMonths;
    private List<Integer> performanceScores;

    // Leave Breakdown
    private long leavesApproved;
    private long leavesPending;
    private long leavesRejected;

    // Onboarding Summary
    private List<?> onboardingList;
}
