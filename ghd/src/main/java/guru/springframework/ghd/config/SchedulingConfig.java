package guru.springframework.ghd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Hạ tầng scheduling ĐẦU TIÊN của project (chưa có @Scheduled nào trước đây). Job thật
// nằm ở PaymentReconciliationServiceImpl - class này chỉ bật tính năng, theo đúng
// pattern CacheConfig (@EnableCaching)/KafkaTopicConfig: bật ngay trên config class
// chuyên trách, không bật trên GhdApplication.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
