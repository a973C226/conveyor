package com.project.conveyor.controller;

import com.project.conveyor.model.CreditDTO;
import com.project.conveyor.model.LoanApplicationRequestDTO;
import com.project.conveyor.model.LoanOfferDTO;
import com.project.conveyor.model.ScoringDataDTO;
import com.project.conveyor.service.ConveyorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name="Conveyor controller", description="The main controller handling all requests")
public class ConveyorController{

    private final ConveyorService conveyorService;

    @PostMapping("/conveyor/offers")
    @Operation(summary = "Calculation of possible loan conditions.",
    description = "Calculation of possible loan conditions, data prescoring.")
    public ResponseEntity<List<LoanOfferDTO>> getLoanOffers(@RequestBody @Parameter(required = true)
                                                                LoanApplicationRequestDTO request){
        return conveyorService.getLoanOffers(request);
    }

    @PostMapping("/conveyor/calculation")
    @Operation(summary = "Calculation of loan parameters.",
            description = "Data validation, data scoring, full calculation of loan parameters.")
    public ResponseEntity<CreditDTO> calculationLoanParams(@RequestBody @Parameter(required = true)
                                                               ScoringDataDTO request){
        return conveyorService.calculationLoanParams(request);
    }

}