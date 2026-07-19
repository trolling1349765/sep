package fpt.capstone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs (currently only BackupSchedulerJob). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
