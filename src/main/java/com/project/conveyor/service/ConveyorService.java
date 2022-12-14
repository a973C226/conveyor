package com.project.conveyor.service;

import com.project.conveyor.model.CreditDTO;
import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.LoanOfferDTO;
import com.project.conveyor.model.ScoringDataDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ConveyorService{

    @NotNull ResponseEntity<List<LoanOfferDTO>> getLoanOffers(@NotNull LoanApplicationRequestDTO request);
    @NotNull ResponseEntity<CreditDTO> calculationLoanParams(@NotNull ScoringDataDTO request);
}
