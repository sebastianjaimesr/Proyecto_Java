package com.littlehorizons.erp.model.extracurricular;

import java.time.LocalDate;

/**
 * Membership of a student in a club ({@code membresias_club}).
 *
 * @param id          {@code null} before the record is inserted
 * @param studentName joined, for display
 * @param clubName    joined, for display
 */
public record ClubMembership(
        Integer id,
        int studentId,
        String studentName,
        int clubId,
        String clubName,
        LocalDate joinDate,
        MembershipStatus status,
        LocalDate withdrawalDate,
        String withdrawalReason) {
}
