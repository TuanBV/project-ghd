package com.example.mcprice.config;

import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.job.CompetitorCrawlJob;
import com.example.mcprice.job.CompetitorDiscoveryJob;
import com.example.mcprice.job.DailyPipelineJob;
import com.example.mcprice.job.ImportRefreshJob;
import com.example.mcprice.job.MerchantSyncJob;
import com.example.mcprice.job.PriceCalculationJob;
import com.example.mcprice.job.StaleObservationCleanupJob;
import com.example.mcprice.job.WebsitePublishJob;
import lombok.RequiredArgsConstructor;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

/**
 * Khai bao JobDetail cho tat ca job (durable, khong mat khi restart nho JDBC JobStore).
 * Chi DailyPipelineJob co Trigger cron mac dinh; cac job atomic khac chi chay khi trigger tay
 * (qua JobService) hoac duoc goi lai trong DailyPipelineJob.
 */
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final AppProperties appProperties;

    private JobDetail buildJobDetail(Class<? extends org.quartz.Job> jobClass, String name) {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(jobClass);
        factory.setName(name);
        factory.setDurability(true);
        factory.setRequestsRecovery(true);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobDetail importRefreshJobDetail() {
        return buildJobDetail(ImportRefreshJob.class, JobKeys.IMPORT_REFRESH);
    }

    @Bean
    public JobDetail competitorCrawlJobDetail() {
        return buildJobDetail(CompetitorCrawlJob.class, JobKeys.COMPETITOR_CRAWL);
    }

    @Bean
    public JobDetail competitorDiscoveryJobDetail() {
        return buildJobDetail(CompetitorDiscoveryJob.class, JobKeys.COMPETITOR_DISCOVERY);
    }

    @Bean
    public JobDetail priceCalculationJobDetail() {
        return buildJobDetail(PriceCalculationJob.class, JobKeys.PRICE_CALCULATION);
    }

    @Bean
    public JobDetail websitePublishJobDetail() {
        return buildJobDetail(WebsitePublishJob.class, JobKeys.WEBSITE_PUBLISH);
    }

    @Bean
    public JobDetail merchantSyncJobDetail() {
        return buildJobDetail(MerchantSyncJob.class, JobKeys.MERCHANT_SYNC);
    }

    @Bean
    public JobDetail staleObservationCleanupJobDetail() {
        return buildJobDetail(StaleObservationCleanupJob.class, JobKeys.STALE_OBSERVATION_CLEANUP);
    }

    @Bean
    public JobDetail dailyPipelineJobDetail() {
        return buildJobDetail(DailyPipelineJob.class, JobKeys.DAILY_PIPELINE);
    }

    @Bean
    public Trigger dailyPipelineTrigger(JobDetail dailyPipelineJobDetail) throws java.text.ParseException {
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(dailyPipelineJobDetail);
        factory.setName(JobKeys.DAILY_PIPELINE + "-cron");
        factory.setCronExpression(appProperties.getJob().getDailyPipelineCron());
        factory.setTimeZone(java.util.TimeZone.getTimeZone(appProperties.getTimezone()));
        factory.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
