package com.project.conveyor.controller;

import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.ScoringDataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConveyorController{

    @PostMapping("/conveyor/offers")
    public ResponseEntity<?> getLoanOffers(@RequestBody LoanApplicationRequestDTO request){
        return null;
    }

    @PostMapping("/conveyor/calculation")
    public ResponseEntity<?> calculationCreditParams(@RequestBody ScoringDataDTO request){
        return null;
    }

}