package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SystemController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.CatalogDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeRecordDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.dao.jdbc.JdbcAttendanceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcCatalogDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGradeRecordDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectRegistrationDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSystemDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherAssignmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTransactionManager;
import com.littlehorizons.erp.model.system.ConfigurationSummary;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.ArithmeticMeanStrategy;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.AverageCalculator;
import com.littlehorizons.erp.service.EnrollmentService;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.SystemService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.ConnectionProvider;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.view.AcademicMenuView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.Menu;
import com.littlehorizons.erp.view.StudentMenuView;
import com.littlehorizons.erp.view.SystemMenuView;
import com.littlehorizons.erp.view.TeacherMenuView;
import com.littlehorizons.erp.view.ViewSupport;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Factory that builds the object graph once: DAOs, services, controllers and
 * views, wired through their constructors (dependency injection without a
 * framework). Concrete JDBC classes are only referenced here.
 */
public final class ApplicationFactory {

    private final ConsoleUtils console;
    private final ConfigurationSummary configurationSummary;
    private final ConnectionProvider connectionProvider;
    private final TransactionManager transactions;
    private final Clock clock;

    // DAOs
    private final StudentDao studentDao = new JdbcStudentDao();
    private final GuardianDao guardianDao = new JdbcGuardianDao();
    private final EnrollmentDao enrollmentDao = new JdbcEnrollmentDao();
    private final CatalogDao catalogDao = new JdbcCatalogDao();
    private final SubjectDao subjectDao = new JdbcSubjectDao();
    private final TeacherDao teacherDao = new JdbcTeacherDao();
    private final TeacherAssignmentDao assignmentDao = new JdbcTeacherAssignmentDao();
    private final SubjectRegistrationDao registrationDao = new JdbcSubjectRegistrationDao();
    private final GradeRecordDao gradeRecordDao = new JdbcGradeRecordDao();
    private final AttendanceDao attendanceDao = new JdbcAttendanceDao();

    public ApplicationFactory(AppConfig config, ConsoleUtils console, ConnectionProvider connectionProvider,
                              ConfigurationSummary configurationSummary) {
        Objects.requireNonNull(config, "config");
        this.console = Objects.requireNonNull(console, "console");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.configurationSummary = Objects.requireNonNull(configurationSummary, "configurationSummary");
        this.transactions = new JdbcTransactionManager(connectionProvider);
        this.clock = Clock.systemDefaultZone();
    }

    public SystemMenuView systemMenuView() {
        SystemService systemService = new SystemService(new JdbcSystemDao(connectionProvider), configurationSummary);
        return new SystemMenuView(console, new SystemController(systemService));
    }

    /** Builds the main menu with every module in the order of the statement. */
    public MainMenuView mainMenuView(SystemMenuView systemMenuView) {
        ViewSupport support = new ViewSupport(console);

        StudentController studentController = new StudentController(
                new StudentService(transactions, studentDao, guardianDao, enrollmentDao, clock),
                new GuardianService(transactions, guardianDao, studentDao),
                new EnrollmentService(transactions, enrollmentDao, studentDao, catalogDao, clock));
        TeacherController teacherController = new TeacherController(
                new TeacherService(transactions, teacherDao, subjectDao, assignmentDao, clock),
                new SubjectService(transactions, subjectDao, catalogDao));
        AcademicController academicController = new AcademicController(
                new AcademicService(transactions, studentDao, enrollmentDao, subjectDao, registrationDao,
                        gradeRecordDao, assignmentDao, new AverageCalculator(new ArithmeticMeanStrategy()), clock),
                new AttendanceService(transactions, attendanceDao, studentDao, subjectDao, registrationDao,
                        assignmentDao, clock));

        StudentMenuView studentView = new StudentMenuView(support, studentController);
        TeacherMenuView teacherView = new TeacherMenuView(support, teacherController);
        AcademicMenuView academicView = new AcademicMenuView(support, academicController, teacherController,
                studentView, teacherView);

        return new MainMenuView(console, List.of(
                new Menu.Item("Gestión de estudiantes y acudientes", studentView::show),
                new Menu.Item("Gestión académica", academicView::show),
                new Menu.Item("Gestión de profesores", teacherView::show),
                MainMenuView.pending(console, "Actividades extracurriculares", 3),
                MainMenuView.pending(console, "Gestión financiera", 4),
                MainMenuView.pending(console, "Gestión de recursos", 5),
                MainMenuView.pending(console, "Reportes y estadísticas", 6),
                new Menu.Item("Configuración y estado del sistema", systemMenuView::show)));
    }
}
