package com.project.conveyor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("prescoring-constants")
public class PrescoringConfiguration {
    private BigDecimal minRequestedAmount;
    private Integer minTerm;
    private Integer minAge;
}
