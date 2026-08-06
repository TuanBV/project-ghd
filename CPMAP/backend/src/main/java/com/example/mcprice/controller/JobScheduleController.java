package com.example.mcprice.controller;

import com.example.mcprice.service.JobScheduleService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/job-schedule")
@RequiredArgsConstructor
public class JobScheduleController {

    private final JobScheduleService jobScheduleService;

    public record CronDto(String cron, String timezone) {
    }

    public record UpdateCronRequest(@NotBlank String cron, @NotBlank String timezone) {
    }

    @GetMapping
    public CronDto get() throws SchedulerException {
        return new CronDto(jobScheduleService.getCurrentCron(), "Asia/Ho_Chi_Minh");
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CronDto update(@RequestBody UpdateCronRequest request) throws SchedulerException {
        jobScheduleService.updateCron(request.cron(), request.timezone());
        return new CronDto(request.cron(), request.timezone());
    }
}
