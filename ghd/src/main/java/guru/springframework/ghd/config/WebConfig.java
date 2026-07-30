package guru.springframework.ghd.config;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${seo.static.path:seo}")
    private String seoStaticPath;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String seoLocation = toFileResourceLocation(seoStaticPath);
        String uploadsLocation = toFileResourceLocation(uploadPath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/common/**")
                .addResourceLocations("classpath:/static/common/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/client/**")
                .addResourceLocations("classpath:/static/client/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/sitemap.xml")
                .addResourceLocations(seoLocation, "classpath:/static/")
                .setCacheControl(CacheControl.noCache());

        registry.addResourceHandler("/robots.txt")
                .addResourceLocations(seoLocation, "classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }

    private String toFileResourceLocation(String directory) {
        Path path = Paths.get(directory).toAbsolutePath().normalize();
        String location = path.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(200));
        factory.setMaxRequestSize(DataSize.ofMegabytes(500));
        return factory.createMultipartConfig();
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers((Connector connector) -> {
            connector.setMaxPostSize(200 * 1024 * 1024);
        });
        return factory;
    }
}
