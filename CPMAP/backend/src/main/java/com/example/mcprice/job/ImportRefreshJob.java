package com.example.mcprice.job;

import com.example.mcprice.config.AppProperties;
import com.example.mcprice.service.ComparisonImportService;
import com.example.mcprice.service.McImportService;
import com.example.mcprice.service.JobRunService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quet thu muc app.importWatchDir (mac dinh data/import) va tu dong import lai neu phat hien
 * file MC/bao cao so sanh gia (idempotent theo file hash nen file khong doi se khong lam gi).
 */
public class ImportRefreshJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(ImportRefreshJob.class);

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private McImportService mcImportService;
    @Autowired
    private ComparisonImportService comparisonImportService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, com.example.mcprice.domain.JobKeys.IMPORT_REFRESH, jobRunId -> {
            File dir = new File(appProperties.getImportWatchDir());
            int total = 0;
            int success = 0;
            int failed = 0;
            File[] files = dir.exists() ? dir.listFiles((f, name) -> name.toLowerCase().endsWith(".xlsx")) : null;
            if (files != null) {
                for (File file : files) {
                    total++;
                    try {
                        classifyAndImport(file);
                        success++;
                    } catch (Exception e) {
                        failed++;
                        log.warn("Khong import duoc file {}: {}", file.getName(), e.getMessage());
                    }
                }
            }
            return new int[] { total, success, failed };
        });
    }

    private void classifyAndImport(File file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            if (workbook.getSheet("Trang tính1") != null) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    mcImportService.importFile(file.getName(), fis, "SYSTEM_JOB");
                }
                return;
            }
            Sheet comparisonSheet = workbook.getSheet("So sánh giá");
            if (comparisonSheet != null) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    comparisonImportService.importFile(file.getName(), fis, "SYSTEM_JOB");
                }
            }
        }
    }
}
