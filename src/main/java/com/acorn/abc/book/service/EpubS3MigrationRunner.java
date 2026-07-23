package com.acorn.abc.book.service;

import com.acorn.abc.AbcApplication;
import com.acorn.abc.book.service.EpubS3MigrationService.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * EPUB S3 one-off migration 전용 entry point.
 *
 * <p>일반 application 기동 경로에서는 절대 실행되지 않는다. 별도 process로만 실행하며
 * {@code app.epub-migration.enabled=true}로 명시적 opt-in해야 실제 작업을 수행한다.
 *
 * <p>실행 예 (bootJar 산출물 기준):
 * <pre>
 * java -Dloader.main=com.acorn.abc.book.service.EpubS3MigrationRunner \
 *      -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher
 * </pre>
 *
 * <p>exit code: 0 성공, 1 failed 1건 이상 또는 예외, 2 opt-in 누락.
 */
public final class EpubS3MigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(EpubS3MigrationRunner.class);

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILED = 1;
    private static final int EXIT_NOT_ENABLED = 2;

    private EpubS3MigrationRunner() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AbcApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        ConfigurableApplicationContext context = application.run(args);
        int exitCode = run(context);
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    static int run(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        if (!environment.getProperty("app.epub-migration.enabled", Boolean.class, false)) {
            log.error("EPUB S3 migration is not enabled. Set app.epub-migration.enabled=true to run it.");
            return EXIT_NOT_ENABLED;
        }

        boolean dryRun = environment.getProperty("app.epub-migration.dry-run", Boolean.class, true);
        int batchSize = environment.getProperty("app.epub-migration.batch-size", Integer.class, 50);

        try {
            EpubS3MigrationService service = context.getBean(EpubS3MigrationService.class);
            MigrationReport report = service.migrate(dryRun, batchSize);
            log.info("EPUB S3 migration report: dryRun={}, candidates={}, migrated={}, skipped={}, "
                            + "failed={}, invariantViolations={}",
                    dryRun, report.candidates(), report.migrated(), report.skipped(), report.failed(),
                    report.invariantViolations());
            // 손상 row(invariant 위반)는 dry-run에서도 failed에 포함되므로 exit code가 non-zero가 된다.
            return report.failed() > 0 ? EXIT_FAILED : EXIT_SUCCESS;
        } catch (RuntimeException e) {
            log.error("EPUB S3 migration aborted: cause={}", e.getClass().getSimpleName());
            return EXIT_FAILED;
        }
    }
}
