package com.example.mcprice.service;

import com.example.mcprice.domain.JobKeys;
import java.util.Map;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

/** Cho phep sua cron cua job tong hang ngay tu Settings, luu ben trong Quartz JDBC JobStore (song qua restart). */
@Service
@RequiredArgsConstructor
public class JobScheduleService {

    private static final String TRIGGER_NAME = JobKeys.DAILY_PIPELINE + "-cron";

    private final Scheduler scheduler;
    private final AuditService auditService;

    public String getCurrentCron() throws SchedulerException {
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(TRIGGER_NAME));
        if (trigger instanceof CronTrigger cronTrigger) {
            return cronTrigger.getCronExpression();
        }
        return null;
    }

    public void updateCron(String cronExpression, String timezone) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(TRIGGER_NAME);
        CronTrigger existing = (CronTrigger) scheduler.getTrigger(triggerKey);
        Trigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(existing.getJobKey())
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone(timezone)))
                .build();
        scheduler.rescheduleJob(triggerKey, newTrigger);
        auditService.record("JOB_SCHEDULE_UPDATE", "JOB_SCHEDULE", JobKeys.DAILY_PIPELINE,
                Map.of("cron", cronExpression, "timezone", timezone));
    }
}
