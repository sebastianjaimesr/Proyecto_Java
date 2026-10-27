package test.controller;

import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.InputUnavailableException;
import com.littlehorizons.erp.util.AppLogger;
import test.dao.AssignedSubjectDao;
import test.dao.TeacherSummaryDao;
import test.model.TeacherSummary;
import test.model.TeachingLoad;
import test.view.SubjectExportView;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SubjectExportController {

    public static final int ACADEMIC_YEAR = 2025;

    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final BigInteger MAX_TEACHER_ID = BigInteger.valueOf(Integer.MAX_VALUE);

    private final TransactionManager transactions;
    private final TeacherSummaryDao teacherDao;
    private final AssignedSubjectDao subjectDao;
    private final SubjectReportExporter exporter;
    private final Clock clock;
    private final SubjectExportView view;

    public SubjectExportController(TransactionManager transactions, TeacherSummaryDao teacherDao,
                                   AssignedSubjectDao subjectDao, SubjectReportExporter exporter,
                                   Clock clock, SubjectExportView view) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.view = Objects.requireNonNull(view, "view");
    }

    public void run() {
        view.showHeader(ACADEMIC_YEAR);
        try {
            TeachingLoad load = requestTeachingLoad();
            if (load.hasSubjects()) {
                export(load);
            } else {
                view.showTeacherWithoutSubjects(load.teacher().fullName(), ACADEMIC_YEAR);
            }
        } catch (InputUnavailableException e) {
            view.showInputClosed();
        } catch (DataAccessException e) {
            AppLogger.logError(SubjectExportController.class, "Could not read the teaching load", e);
            view.showDatabaseError();
        }
    }

    static Optional<BigInteger> parseInteger(String input) {
        String text = input == null ? "" : input.strip();
        return INTEGER.matcher(text).matches() ? Optional.of(new BigInteger(text)) : Optional.empty();
    }

    private TeachingLoad requestTeachingLoad() {
        while (true) {
            Optional<BigInteger> teacherId = parseInteger(view.readTeacherId());
            if (teacherId.isEmpty()) {
                view.showInvalidNumber();
                continue;
            }
            Optional<TeachingLoad> load = findTeachingLoad(teacherId.get());
            if (load.isPresent()) {
                return load.get();
            }
            view.showTeacherNotFound(teacherId.get().toString());
        }
    }

    private Optional<TeachingLoad> findTeachingLoad(BigInteger teacherId) {
        if (teacherId.signum() <= 0 || teacherId.compareTo(MAX_TEACHER_ID) > 0) {
            return Optional.empty();
        }
        int id = teacherId.intValueExact();
        return transactions.read(connection -> loadTeachingLoad(connection, id));
    }

    private Optional<TeachingLoad> loadTeachingLoad(Connection connection, int teacherId) throws SQLException {
        Optional<TeacherSummary> teacher = teacherDao.findById(connection, teacherId);
        if (teacher.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TeachingLoad(teacher.get(), ACADEMIC_YEAR,
                subjectDao.findByTeacherAndYear(connection, teacherId, ACADEMIC_YEAR)));
    }

    private void export(TeachingLoad load) {
        try {
            exporter.export(load, LocalDate.now(clock));
            view.showExportSucceeded(load.teacher().fullName(), load.exportFileName());
        } catch (IOException | SecurityException e) {
            AppLogger.logError(SubjectExportController.class, "Could not write " + load.exportFileName(), e);
            view.showExportFailed();
        }
    }
}
