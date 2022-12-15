package com.project.conveyor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("default-setting")
public class BasicConfiguration {
    private BigDecimal defaultRate;
    private long applicationId;
}
