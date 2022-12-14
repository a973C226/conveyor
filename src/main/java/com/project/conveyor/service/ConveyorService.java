package com.project.conveyor.service;

import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.ScoringDataDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

public interface ConveyorService{

    @NotNull ResponseEntity<?> getLoanOffers(@NotNull LoanApplicationRequestDTO request);
    @NotNull ResponseEntity<?> calculationLoanParams(@NotNull ScoringDataDTO request);
}
