package com.project.conveyor.controller;

import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.ScoringDataDTO;
import com.project.conveyor.service.ConveyorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConveyorController{

    @Autowired(required = false)
    private ConveyorService conveyorService;

    @PostMapping("/conveyor/offers")
    public ResponseEntity<?> getLoanOffers(@RequestBody LoanApplicationRequestDTO request){
        return conveyorService.getLoanOffers(request);
    }

    @PostMapping("/conveyor/calculation")
    public ResponseEntity<?> calculationLoanParams(@RequestBody ScoringDataDTO request){
        return conveyorService.calculationLoanParams(request);
    }

}