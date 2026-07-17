package com.riigiluup;

import com.riigiluup.ingestion.rahvaalgatus.RahvaalgatusProperties;
import com.riigiluup.ingestion.riigikogu.RiigikoguProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({RiigikoguProperties.class, RahvaalgatusProperties.class})
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
