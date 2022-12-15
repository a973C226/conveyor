package com.project.conveyor.controller;

import com.project.conveyor.model.CreditDTO;
import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.LoanOfferDTO;
import com.project.conveyor.model.ScoringDataDTO;
import com.project.conveyor.service.ConveyorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ConveyorController{

    private final ConveyorService conveyorService;

    @PostMapping("/conveyor/offers")
    public ResponseEntity<List<LoanOfferDTO>> getLoanOffers(@RequestBody LoanApplicationRequestDTO request){
        return conveyorService.getLoanOffers(request);
    }

    @PostMapping("/conveyor/calculation")
    public ResponseEntity<CreditDTO> calculationLoanParams(@RequestBody ScoringDataDTO request){
        return conveyorService.calculationLoanParams(request);
    }

}