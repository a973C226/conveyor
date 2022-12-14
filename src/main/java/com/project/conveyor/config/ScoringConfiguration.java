package com.project.conveyor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties("scoring-constants")
public class ScoringConfiguration {
    private BigDecimal loanToSalaryRatio;
    private int minAge;
    private int maxAge;
    private int minWorkExperienceTotal;
    private int minWorkExperienceCurrent;
}
