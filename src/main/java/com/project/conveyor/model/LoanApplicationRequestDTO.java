package com.project.conveyor.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@Schema(description = "Data for calculating possible loan conditions.")
public class LoanApplicationRequestDTO {

    @Min(10000)
    private BigDecimal amount;

    @Min(6)
    private Integer term;

    @Size(min = 2, max = 30)
    private String firstName;

    @Size(min = 2, max = 30)
    private String lastName;

    @Size(min = 2, max = 30)
    private String middleName;

    @Pattern(regexp = "[\\w\\.]{2,50}@[\\w\\.]{2,20}")
    private String email;
    private LocalDate birthdate;

    @Pattern(regexp = "[0-9]{4}")
    private String passportSeries;

    @Pattern(regexp = "[0-9]{6}")
    private String passportNumber;
}