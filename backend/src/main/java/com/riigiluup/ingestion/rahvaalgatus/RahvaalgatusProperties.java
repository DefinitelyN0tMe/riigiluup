package com.riigiluup.ingestion.rahvaalgatus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riigiluup.rahvaalgatus")
public record RahvaalgatusProperties(String baseUrl) {
}
