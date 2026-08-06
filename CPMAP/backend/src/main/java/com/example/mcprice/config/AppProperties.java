package com.example.mcprice.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String timezone = "Asia/Ho_Chi_Minh";
    private String currency = "VND";
    private String importWatchDir = "data/import";

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Security security = new Security();
    private final Pricing pricing = new Pricing();
    private final Publish publish = new Publish();
    private final Job job = new Job();
    private final WooCommerce woocommerce = new WooCommerce();

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Data
    public static class Jwt {
        private String secret;
        private int accessTokenMinutes = 30;
        private int refreshTokenDays = 7;
    }

    @Data
    public static class Security {
        private List<String> allowedCrawlDomains = List.of();
        private int maxRedirects = 3;
        private int crawlTimeoutSeconds = 10;
        private String crawlUserAgent = "McPriceOptimizerBot/1.0";
    }

    @Data
    public static class Pricing {
        private int defaultMinimumCompetitorCount = 2;
        private int defaultMaxObservationAgeHours = 48;
        private long defaultRoundingStep = 10000;
        private double defaultMaxIncreasePercent = 15;
        private double defaultMaxDecreasePercent = 15;
        private double defaultOutlierThresholdPercent = 30;
    }

    @Data
    public static class Publish {
        private final Website website = new Website();
        private final Merchant merchant = new Merchant();

        @Data
        public static class Website {
            private boolean dryRun = true;
            private String provider = "MOCK";
        }

        @Data
        public static class Merchant {
            private boolean dryRun = true;
            private String provider = "MOCK";
            private String accountId;
            private String dataSourceId;
            private String credentialsPath;
        }
    }

    @Data
    public static class Job {
        private String dailyPipelineCron = "0 0 2 * * ?";
    }

    @Data
    public static class WooCommerce {
        private boolean enabled = false;
        private String baseUrl;
        private String consumerKey;
        private String consumerSecret;
    }
}
