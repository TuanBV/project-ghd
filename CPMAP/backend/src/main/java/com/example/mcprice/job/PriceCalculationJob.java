package com.example.mcprice.job;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.service.JobRunService;
import com.example.mcprice.service.PriceCalculationService;
import com.example.mcprice.repository.ProductRepository;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class PriceCalculationJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(PriceCalculationJob.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PriceCalculationService priceCalculationService;
    @Autowired
    private JobRunService jobRunService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobExecutionHelper.run(context, jobRunService, JobKeys.PRICE_CALCULATION, jobRunId -> {
            var productIds = productRepository.findAll().stream().map(p -> p.getId()).toList();
            int success = 0;
            int failed = 0;
            for (int i = 0; i < productIds.size(); i++) {
                try {
                    priceCalculationService.calculateForProduct(productIds.get(i));
                    success++;
                } catch (Exception e) {
                    failed++;
                    log.warn("Tinh gia loi cho product #{}: {}", productIds.get(i), e.getMessage());
                }
                if ((i + 1) % 100 == 0) {
                    jobRunService.updateProgress(jobRunId, productIds.size(), success, failed);
                }
            }
            return new int[] { productIds.size(), success, failed };
        });
    }
}
