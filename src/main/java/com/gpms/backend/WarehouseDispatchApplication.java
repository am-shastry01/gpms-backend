package com.gpms.backend;

import com.gpms.backend.config.BootstrapProperties;
import com.gpms.backend.config.JwtProperties;
import com.gpms.backend.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableConfigurationProperties({
        JwtProperties.class,
        MinioProperties.class,
        BootstrapProperties.class,
        com.gpms.backend.config.SmsProperties.class,
        com.gpms.backend.config.AppProperties.class
})
public class WarehouseDispatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseDispatchApplication.class, args);
    }
}
