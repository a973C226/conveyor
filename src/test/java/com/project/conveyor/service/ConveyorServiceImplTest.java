package com.project.conveyor.service;

import com.project.conveyor.exception.PrescoringException;
import com.project.conveyor.exception.ScoringException;
import com.project.conveyor.model.*;
import com.project.conveyor.model.enums.EmploymentStatus;
import com.project.conveyor.model.enums.Gender;
import com.project.conveyor.model.enums.MaritalStatus;
import com.project.conveyor.model.enums.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootTest
class ConveyorServiceImplTest {

    @Autowired
    private ConveyorServiceImpl conveyorService;

    @Test
    void getLoanOffers() {

        LoanApplicationRequestDTO inputData = new LoanApplicationRequestDTO(BigDecimal.valueOf(100000),
                6,
                "Anton",
                "Gusev",
                "Olegovich",
                "cde120502@yandex.ru",
                LocalDate.parse("2002-12-22"),
                "1234",
                "124562");

        ResponseEntity<List<LoanOfferDTO>> result = conveyorService.getLoanOffers(inputData);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(200, result.getStatusCodeValue());
        Assertions.assertEquals(4, Objects.requireNonNull(result.getBody()).size());
    }

    @Test
    void getLoanOffersPrescoringFailed() {

        LoanApplicationRequestDTO inputData = new LoanApplicationRequestDTO(BigDecimal.valueOf(100), // invalid
                5, // invalid
                "Anton1", // invalid
                "Gusev3", // invalid
                "Olegovich2", // invalid
                "cde120502yandex.ru", // invalid
                LocalDate.parse("2010-12-22"), // invalid
                "12342", // invalid
                "1245624"); // invalid

        List<ExceptionReasons> expectedReasons = new ArrayList<>();

        expectedReasons.add(new ExceptionReasons("firstName",
                "Invalid first name. (from 2 to 30 Latin letters)"));
        expectedReasons.add(new ExceptionReasons("lastName",
                "Invalid last name. (from 2 to 30 Latin letters)"));
        expectedReasons.add(new ExceptionReasons("middleName",
                "Invalid middle name. (from 2 to 30 Latin letters)"));
        expectedReasons.add(new ExceptionReasons("amount",
                "Invalid amount. (a real number greater than or equal to 10000)"));
        expectedReasons.add(new ExceptionReasons("term",
                "Invalid term. (an integer greater than or equal to 6)"));
        expectedReasons.add(new ExceptionReasons("birthdate",
                "Invalid birthdate. (in the format yyyy-mm-dd, no later than 18 years from the current day)"));
        expectedReasons.add(new ExceptionReasons("email",
                "Invalid email."));
        expectedReasons.add(new ExceptionReasons("passportSeries",
                "Invalid passport series. (4 digits)"));
        expectedReasons.add(new ExceptionReasons("passportNumber",
                "Invalid passport number. (6 digits)"));

        PrescoringException thrown = Assertions.assertThrows(PrescoringException.class, () -> {
            conveyorService.getLoanOffers(inputData);
        }, "PrescoringException was expected.");
        Assertions.assertEquals(9, thrown.getReasons().size());
        Assertions.assertEquals(expectedReasons, thrown.getReasons());
    }

    @Test
    void calculationLoanParams() {

        EmploymentDTO employment = new EmploymentDTO(EmploymentStatus.SELF_EMPLOYED,
                "124578",
                BigDecimal.valueOf(200000.0),
                Position.TOP_MANAGER,
                12,
                12);

        ScoringDataDTO inputData = new ScoringDataDTO(BigDecimal.valueOf(100000.0),
                12,
                "Anton",
                "Gusev",
                "Olegovich",
                Gender.MALE,
                LocalDate.parse("2002-12-05"),
                "9123",
                "124568",
                LocalDate.parse("2002-12-05"),
                "abc",
                MaritalStatus.MARRIED,
                12,
                employment,
                "1246797867564534233",
                true,
                true);

        ResponseEntity<CreditDTO> result = conveyorService.calculationLoanParams(inputData);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(200, result.getStatusCodeValue());
        Assertions.assertEquals(12, Objects.requireNonNull(result.getBody()).getPaymentSchedule().size());
    }

    @Test
    void calculationLoanParamsScoringFailed() {

        EmploymentDTO employment = new EmploymentDTO(EmploymentStatus.UNEMPLOYED, // invalid
                "124578",
                BigDecimal.valueOf(200.0), // invalid
                Position.TOP_MANAGER,
                11, // invalid
                2); // invalid

        ScoringDataDTO inputData = new ScoringDataDTO(BigDecimal.valueOf(100000.0),
                12,
                "Anton",
                "Gusev",
                "Olegovich",
                Gender.MALE,
                LocalDate.parse("2010-12-05"), // invalid
                "9123",
                "124568",
                LocalDate.parse("2002-12-05"),
                "abc",
                MaritalStatus.MARRIED,
                12,
                employment,
                "1246797867564534233",
                true,
                true);

        List<ExceptionReasons> expectedReasons = new ArrayList<>();

        expectedReasons.add(new ExceptionReasons("employmentStatus",
                "You're employment status is UNEMPLOYED."));
        expectedReasons.add(new ExceptionReasons("amount/salary",
                "The loan amount is more than 20 salaries."));
        expectedReasons.add(new ExceptionReasons("birthdate",
                "Age less than 20 or more than 60 years."));
        expectedReasons.add(new ExceptionReasons("workExperienceTotal",
                "Total experience less than 12 months."));
        expectedReasons.add(new ExceptionReasons("workExperienceCurrent",
                "Current experience less than 3 months."));

        ScoringException thrown = Assertions.assertThrows(ScoringException.class, () -> {
            conveyorService.calculationLoanParams(inputData);
        }, "ScoringException was expected.");
        Assertions.assertEquals(5, thrown.getReasons().size());
        Assertions.assertEquals(expectedReasons, thrown.getReasons());
    }
}