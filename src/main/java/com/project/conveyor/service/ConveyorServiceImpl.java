package com.project.conveyor.service;

import com.project.conveyor.config.BasicConfiguration;
import com.project.conveyor.model.*;
import com.project.conveyor.model.enums.EmploymentStatus;
import com.project.conveyor.model.enums.Gender;
import com.project.conveyor.model.enums.MaritalStatus;
import com.project.conveyor.model.enums.Position;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConfigurationProperties
public class ConveyorServiceImpl implements ConveyorService {

    @Autowired
    private BasicConfiguration basicConfiguration;

    private LoanOfferDTO createLoanOfferDTO(Long applicationId,
                                            BigDecimal requestedAmount,
                                            Integer term,
                                            Boolean isInsuranceEnabled,
                                            Boolean isSalaryClient) {

        BigDecimal rate = basicConfiguration.getDefaultRate();

        if (isInsuranceEnabled) {
            rate = rate.subtract(BigDecimal.valueOf(3.0)); // rate - 3.0
        }
        if (isSalaryClient) {
            rate = rate.subtract(BigDecimal.valueOf(1.0)); // rate - 1.0
        }

        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12 * 100), 20, RoundingMode.HALF_EVEN);
        BigDecimal bracketsInFormula = monthlyRate.add(BigDecimal.valueOf(1)).pow(term);
        BigDecimal numeratorAnnuityRatioCalculation = monthlyRate.multiply(bracketsInFormula);
        BigDecimal denominatorAnnuityRatioCalculation = bracketsInFormula.subtract(BigDecimal.valueOf(1));
        BigDecimal annuityRatio =
                numeratorAnnuityRatioCalculation.divide(denominatorAnnuityRatioCalculation, 20, RoundingMode.HALF_EVEN);
        BigDecimal monthlyPayment = requestedAmount.multiply(annuityRatio).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalAmount = monthlyPayment.multiply(BigDecimal.valueOf(term));
        if (isInsuranceEnabled) {
            BigDecimal insurance = requestedAmount.multiply(BigDecimal.valueOf(0.1)); // insurance = requestedAmount * 10%
            totalAmount = totalAmount.add(insurance);
        }

        return new LoanOfferDTO(
                applicationId,
                requestedAmount,
                totalAmount,
                term,
                monthlyPayment,
                rate,
                isInsuranceEnabled,
                isSalaryClient
        );
    }

    public @NotNull ResponseEntity<?> getLoanOffers(@NotNull LoanApplicationRequestDTO request) {
        long applicationId = 432;
        List<LoanOfferDTO> loanOfferDTOList = new ArrayList<>();

        BigDecimal requestedAmount = request.getAmount();
        int term = request.getTerm();

        LoanOfferDTO firstOffer = createLoanOfferDTO(applicationId,
                requestedAmount,
                term,
                true,
                true);
        loanOfferDTOList.add(firstOffer);

        LoanOfferDTO secondOffer = createLoanOfferDTO(applicationId,
                requestedAmount,
                term,
                true,
                false);
        loanOfferDTOList.add(secondOffer);

        LoanOfferDTO thirdOffer = createLoanOfferDTO(applicationId,
                requestedAmount,
                term,
                false,
                true);
        loanOfferDTOList.add(thirdOffer);

        LoanOfferDTO fourthOffer = createLoanOfferDTO(applicationId,
                requestedAmount,
                term,
                false,
                false);
        loanOfferDTOList.add(fourthOffer);

        return ResponseEntity.ok(loanOfferDTOList);

    }

    private List<PaymentScheduleElement> paymentScheduleCalculation(BigDecimal amount,
                                                                    BigDecimal rate,
                                                                    Integer term,
                                                                    BigDecimal monthlyPayment,
                                                                    LocalDate dateFirstPayment) {

        List<PaymentScheduleElement> paymentSchedule = new ArrayList<>();
        LocalDate datePayment = dateFirstPayment;
        BigDecimal remainingDebt = amount;

        for (int i = 0; i < term; i++) {

            int lengthMonth = datePayment.minusMonths(1).lengthOfMonth();


            BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 20, RoundingMode.HALF_EVEN);
            BigDecimal dailyCoef = new BigDecimal(0);

            // dailyCoef is lengthMonth / lengthYear
            if (datePayment.getMonthValue() == 1 && i > 0) {
                boolean isLeapBeforeYear = datePayment.minusYears(1).isLeapYear();
                int lengthBeforeYear = (isLeapBeforeYear) ? 366: 365;
                boolean isLeapNowYear = datePayment.isLeapYear();
                int lengthNowYear = (isLeapNowYear) ? 366: 365;

                LocalDate monthBefore = datePayment.minusMonths(1);
                int daysUntilNextMonth = monthBefore.until(datePayment.withDayOfMonth(1)).getDays() - 1;
                int daysInNowMonth = datePayment.withDayOfMonth(1).until(datePayment).getDays() + 1;

                BigDecimal firstPartDailyCoef =
                        BigDecimal.valueOf(daysUntilNextMonth).divide(BigDecimal.valueOf(lengthBeforeYear), 20, RoundingMode.HALF_EVEN);
                BigDecimal secondPartDailyCoef =
                        BigDecimal.valueOf(daysInNowMonth).divide(BigDecimal.valueOf(lengthNowYear), 20, RoundingMode.HALF_EVEN);
                dailyCoef = firstPartDailyCoef.add(secondPartDailyCoef);
            }
            else {
                boolean isLeap = datePayment.isLeapYear();
                int lengthYear = (isLeap) ? 366: 365;
                dailyCoef = BigDecimal.valueOf(lengthMonth).divide(BigDecimal.valueOf(lengthYear), 20, RoundingMode.HALF_EVEN);
            }
            BigDecimal interestPayment =
                    remainingDebt.multiply(rateDecimal).multiply(dailyCoef).setScale(2, RoundingMode.HALF_EVEN);

            if (i == term - 1) {
                monthlyPayment = remainingDebt.add(interestPayment);
            }

            BigDecimal debtPayment = monthlyPayment.subtract(interestPayment);
            remainingDebt = remainingDebt.subtract(debtPayment);

            if (i == term - 1) {
                remainingDebt = BigDecimal.valueOf(0);
            }

            PaymentScheduleElement payment = new PaymentScheduleElement(i + 1,
                    datePayment,
                    monthlyPayment,
                    interestPayment,
                    debtPayment,
                    remainingDebt);

            datePayment = datePayment.plusMonths(1);

            paymentSchedule.add(payment);
        }

        return paymentSchedule;
    }

    private CreditDTO createCreditDTO(BigDecimal amount,
                                      Integer term,
                                      BigDecimal rate,
                                      Boolean isInsuranceEnabled,
                                      Boolean isSalaryClient) {

        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12 * 100), 20, RoundingMode.HALF_EVEN);
        BigDecimal bracketsInFormula = monthlyRate.add(BigDecimal.valueOf(1)).pow(term);
        BigDecimal numeratorAnnuityRatioCalculation = monthlyRate.multiply(bracketsInFormula);
        BigDecimal denominatorAnnuityRatioCalculation = bracketsInFormula.subtract(BigDecimal.valueOf(1));
        BigDecimal annuityRatio = numeratorAnnuityRatioCalculation.divide(denominatorAnnuityRatioCalculation, 20, RoundingMode.HALF_EVEN);
        BigDecimal monthlyPayment = amount.multiply(annuityRatio).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal psk = monthlyPayment.multiply(BigDecimal.valueOf(term)).subtract(amount)
                .divide(amount, 20, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN);

        LocalDate dateFirstPayment = LocalDate.now().plusMonths(1);

        return new CreditDTO(
                amount,
                term,
                monthlyPayment,
                rate,
                psk,
                isInsuranceEnabled,
                isSalaryClient,
                paymentScheduleCalculation(amount,
                        rate,
                        term,
                        monthlyPayment,
                        dateFirstPayment)
        );
    }

    public ResponseEntity<?> calculationLoanParams(@NotNull ScoringDataDTO request) {

        BigDecimal defaultRate = basicConfiguration.getDefaultRate();

        EmploymentDTO employment = request.getEmployment();
        BigDecimal rate = defaultRate;

        EmploymentStatus employmentStatus = employment.getEmploymentStatus();
        if (employmentStatus == EmploymentStatus.SELF_EMPLOYED) {
            rate = rate.add(BigDecimal.valueOf(1));
        }
        if (employmentStatus == EmploymentStatus.BUSINESS_OWNER) {
            rate = rate.add(BigDecimal.valueOf(3));
        }

        Position position = employment.getPosition();
        if (position == Position.MIDDLE_MANAGER) {
            rate = rate.subtract(BigDecimal.valueOf(2));
        }
        if (position == Position.TOP_MANAGER) {
            rate = rate.subtract(BigDecimal.valueOf(4));
        }

        MaritalStatus maritalStatus = request.getMaritalStatus();
        if (maritalStatus == MaritalStatus.MARRIED) {
            rate = rate.subtract(BigDecimal.valueOf(3));
        }
        if (maritalStatus == MaritalStatus.DIVORCED) {
            rate = rate.add(BigDecimal.valueOf(1));
        }

        if (request.getDependentAmount() > 1) {
            rate = rate.add(BigDecimal.valueOf(1));
        }

        Gender gender = request.getGender();
        LocalDate birthDate = request.getBirthdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        if (gender == Gender.FEMALE && age >= 35 && age <= 60) {
            rate = rate.subtract(BigDecimal.valueOf(3));
        }
        if (gender == Gender.MALE && age >= 30 && age <= 55) {
            rate = rate.subtract(BigDecimal.valueOf(3));
        }
        if (gender == Gender.NON_BINARY) {
            rate = rate.add(BigDecimal.valueOf(3));
        }

        BigDecimal amount = request.getAmount();
        int term = request.getTerm();
        boolean isInsuranceEnabled = request.getIsInsuranceEnabled();
        boolean isSalaryClient = request.getIsSalaryClient();

        CreditDTO response = createCreditDTO(amount, term, rate, isInsuranceEnabled, isSalaryClient);
        return ResponseEntity.ok(response);

    }
}