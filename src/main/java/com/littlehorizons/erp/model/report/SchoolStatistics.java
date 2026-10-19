package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;

/**
 * Cifras generales del colegio en un año.
 */
public record SchoolStatistics(
        int academicYear,
        int activeStudents,
        int activeEnrollments,
        int activeTeachers,
        int guardians,
        int activeClubs,
        int activeClubMembers,
        int events,
        int eventParticipations,
        int resources,
        int loanedResources,
        int openLoans,
        BigDecimal paymentsTotal,
        int paymentsCount,
        BigDecimal averageGrade,
        int gradeRecords,
        int absences) {
}
