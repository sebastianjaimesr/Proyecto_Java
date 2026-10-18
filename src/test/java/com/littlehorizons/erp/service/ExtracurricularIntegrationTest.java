package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcClubDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEventDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.academic.Gender;
import com.littlehorizons.erp.model.academic.Student;
import com.littlehorizons.erp.model.academic.StudentDocumentType;
import com.littlehorizons.erp.model.academic.StudentStatus;
import com.littlehorizons.erp.model.common.PersonName;
import com.littlehorizons.erp.model.extracurricular.Club;
import com.littlehorizons.erp.model.extracurricular.ClubMembership;
import com.littlehorizons.erp.model.extracurricular.CompetitionPosition;
import com.littlehorizons.erp.model.extracurricular.Event;
import com.littlehorizons.erp.model.extracurricular.EventParticipant;
import com.littlehorizons.erp.model.extracurricular.EventType;
import com.littlehorizons.erp.model.extracurricular.MembershipStatus;
import com.littlehorizons.erp.model.extracurricular.ParticipationRole;
import com.littlehorizons.erp.support.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clubs and events against the real MySQL test database.
 */
class ExtracurricularIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(500);
    private static final LocalDate JOIN_DATE = LocalDate.of(2026, 2, 10);

    private static StudentService students;
    private static ClubService clubs;
    private static EventService events;

    @BeforeAll
    static void createDatabase() {
        TestDatabase database = TestDatabase.recreate();
        TransactionManager transactions = database.transactionManager();
        JdbcStudentDao studentDao = new JdbcStudentDao();
        students = new StudentService(transactions, studentDao, new JdbcGuardianDao(), new JdbcEnrollmentDao(),
                TestDatabase.CLOCK);
        clubs = new ClubService(transactions, new JdbcClubDao(), studentDao, new JdbcTeacherDao(), TestDatabase.CLOCK);
        events = new EventService(transactions, new JdbcEventDao(), studentDao, new JdbcTeacherDao());
    }

    private static Student newStudent() {
        int n = SEQUENCE.incrementAndGet();
        return students.register(new Student(null, StudentDocumentType.TI, "TI" + (2_000_000 + n),
                new PersonName("Mateo", null, "Club" + n, null), LocalDate.of(2013, 4, 4), Gender.MALE, null, null,
                null, null, null, null, LocalDate.of(2020, 1, 20), StudentStatus.ACTIVE));
    }

    private static Club newClub(int capacity) {
        int n = SEQUENCE.incrementAndGet();
        return clubs.create(new Club(null, "Club de prueba " + n, "Descripción", null, null, capacity,
                "Martes 3-5 PM", true, 0));
    }

    @Test
    void capacityAndDuplicateMembershipsAreEnforced() {
        Club club = newClub(2);
        Student first = newStudent();
        Student second = newStudent();
        Student third = newStudent();

        clubs.enroll(first.id(), club.id(), JOIN_DATE);
        assertThrows(BusinessRuleException.class, () -> clubs.enroll(first.id(), club.id(), JOIN_DATE),
                "duplicate active membership");
        clubs.enroll(second.id(), club.id(), JOIN_DATE);
        BusinessRuleException full = assertThrows(BusinessRuleException.class,
                () -> clubs.enroll(third.id(), club.id(), JOIN_DATE));
        assertTrue(full.getMessage().contains("cupos"));
        assertThrows(BusinessRuleException.class, () -> clubs.update(new Club(club.id(), club.name(), null, null,
                null, 1, null, true, 0)), "capacity below current members");
    }

    @Test
    void withdrawnMembershipFreesTheSeatAndStaysInHistory() {
        Club club = newClub(1);
        Student first = newStudent();
        Student second = newStudent();
        ClubMembership membership = clubs.enroll(first.id(), club.id(), JOIN_DATE);

        clubs.withdraw(membership.id(), LocalDate.of(2026, 5, 1), "Cambio de horario");
        clubs.enroll(second.id(), club.id(), LocalDate.of(2026, 5, 2));

        List<ClubMembership> history = clubs.findByStudent(first.id());
        assertEquals(1, history.size());
        assertEquals(MembershipStatus.WITHDRAWN, history.get(0).status());
        assertEquals(1, clubs.findMembers(club.id(), true).size());
        assertThrows(ValidationException.class,
                () -> clubs.enroll(first.id(), club.id(), TestDatabase.TODAY.plusDays(3)));
    }

    @Test
    @DisplayName("Simultaneous enrollments never exceed the capacity (SELECT ... FOR UPDATE)")
    void concurrentEnrollmentsRespectCapacity() throws Exception {
        Club club = newClub(3);
        List<Student> candidates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            candidates.add(newStudent());
        }
        ExecutorService pool = Executors.newFixedThreadPool(candidates.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (Student student : candidates) {
            Callable<Boolean> task = () -> {
                start.await();
                try {
                    clubs.enroll(student.id(), club.id(), JOIN_DATE);
                    return true;
                } catch (BusinessRuleException e) {
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        int accepted = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                accepted++;
            }
        }
        pool.shutdown();

        assertEquals(3, accepted);
        assertEquals(3, clubs.findMembers(club.id(), true).size());
    }

    @Test
    void competitionPositionsAreValidated() {
        Event competition = events.create(new Event(null, "Olimpiada de Matemáticas " + SEQUENCE.incrementAndGet(),
                null, EventType.ACADEMIC, LocalDate.of(2026, 9, 20), "Auditorio", true, null, null, true));
        Event party = events.create(new Event(null, "Día de la Familia " + SEQUENCE.incrementAndGet(), null,
                EventType.SOCIAL, LocalDate.of(2026, 5, 15), "Patio", false, null, null, true));
        Student winner = newStudent();
        Student runnerUp = newStudent();

        events.addParticipant(competition.id(), winner.id(), ParticipationRole.PARTICIPANT, null);
        events.addParticipant(competition.id(), runnerUp.id(), ParticipationRole.COMPETITOR, null);
        assertThrows(BusinessRuleException.class,
                () -> events.addParticipant(competition.id(), winner.id(), ParticipationRole.ATTENDEE, null));

        EventParticipant first = events.recordResult(competition.id(), winner.id(), CompetitionPosition.FIRST, null);
        assertEquals(ParticipationRole.COMPETITOR, first.role());
        assertThrows(BusinessRuleException.class,
                () -> events.recordResult(competition.id(), runnerUp.id(), CompetitionPosition.FIRST, null));
        events.recordResult(competition.id(), runnerUp.id(), CompetitionPosition.SECOND, null);

        events.addParticipant(party.id(), winner.id(), ParticipationRole.ATTENDEE, null);
        assertThrows(BusinessRuleException.class,
                () -> events.recordResult(party.id(), winner.id(), CompetitionPosition.FIRST, null));

        assertEquals(2, events.findByStudent(winner.id()).size());
        List<EventParticipant> results = events.findResults(2026).stream()
                .filter(item -> item.eventId() == competition.id()).toList();
        assertEquals(List.of(CompetitionPosition.FIRST, CompetitionPosition.SECOND),
                results.stream().map(EventParticipant::position).toList());
    }
}
