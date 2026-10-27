package test;

import com.littlehorizons.erp.dao.jdbc.JdbcTransactionManager;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.util.AppConfig;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtils;
import com.littlehorizons.erp.util.DatabaseConnection;
import test.controller.AtomicTextFileWriter;
import test.controller.SubjectExportController;
import test.controller.SubjectReportExporter;
import test.controller.SubjectReportFormatter;
import test.dao.JdbcAssignedSubjectDao;
import test.dao.JdbcTeacherSummaryDao;
import test.view.SubjectExportView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.logging.Logger;

public final class Main {

    private static final String LOGS_DIR_KEY = "logs.dir";
    private static final String DEFAULT_LOGS_DIR = "logs";
    private static final String CONSOLE_ENCODING_KEY = "console.encoding";
    private static final Logger EXPORTER_LOGGER = Logger.getLogger(Main.class.getPackageName());

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = null;
        ConfigurationException configurationError = null;
        try {
            config = AppConfig.getInstance();
        } catch (ConfigurationException e) {
            configurationError = e;
        }
        String encoding = config == null
                ? ConsoleUtils.AUTO_ENCODING
                : config.getOptional(CONSOLE_ENCODING_KEY, ConsoleUtils.AUTO_ENCODING);

        try (ConsoleUtils console = ConsoleUtils.forSystemConsole(encoding)) {
            SubjectExportView view = new SubjectExportView(console);
            if (configurationError != null) {
                view.showConfigurationError(configurationError.getMessage());
                return;
            }
            run(config, view);
        }
    }

    private static void run(AppConfig config, SubjectExportView view) {
        try {
            AppLogger.init(Paths.get(config.getOptional(LOGS_DIR_KEY, DEFAULT_LOGS_DIR)));
            EXPORTER_LOGGER.setParent(Logger.getLogger(AppLogger.ROOT_LOGGER_NAME));
            createController(view).run();
        } catch (ConfigurationException e) {
            AppLogger.logError(Main.class, "Configuration error at start-up", e);
            view.showConfigurationError(e.getMessage());
        } catch (RuntimeException e) {
            AppLogger.logError(Main.class, "Unexpected error in the subject exporter", e);
            view.showUnexpectedError();
        } finally {
            AppLogger.shutdown();
        }
    }

    private static SubjectExportController createController(SubjectExportView view) {
        Path projectRoot = Paths.get("").toAbsolutePath();
        SubjectReportExporter exporter = new SubjectReportExporter(
                new SubjectReportFormatter(), new AtomicTextFileWriter(), projectRoot);
        return new SubjectExportController(
                new JdbcTransactionManager(DatabaseConnection.getInstance()),
                new JdbcTeacherSummaryDao(),
                new JdbcAssignedSubjectDao(),
                exporter,
                Clock.systemDefaultZone(),
                view);
    }
}
