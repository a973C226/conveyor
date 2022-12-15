package com.project.conveyor.service;

import com.project.conveyor.config.BasicConfiguration;
import com.project.conveyor.config.PrescoringConfiguration;
import com.project.conveyor.config.ScoringConfiguration;
import com.project.conveyor.exception.PrescoringException;
import com.project.conveyor.exception.ScoringException;
import com.project.conveyor.model.*;
import com.project.conveyor.model.enums.EmploymentStatus;
import com.project.conveyor.model.enums.Gender;
import com.project.conveyor.model.enums.MaritalStatus;
import com.project.conveyor.model.enums.Position;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
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

    private final BasicConfiguration basicConfiguration;
    private final PrescoringConfiguration prescoringConfiguration;
    private final ScoringConfiguration scoringConfiguration;

    private void prescoring(LoanApplicationRequestDTO request) {

        List<ExceptionReasons> reasons = new ArrayList<>();

        String firstName = request.getFirstName();
        boolean checkFirstName = firstName.matches("^[a-zA-Z]{2,30}");
        if (!checkFirstName) {
            reasons.add(new ExceptionReasons("firstName",
                    "Invalid first name. (from 2 to 30 Latin letters)"));
        }

        String lastName = request.getLastName();
        boolean checkLastName =
                lastName.matches("^[a-zA-Z]{2,30}");
        if (!checkLastName) {
            reasons.add(new ExceptionReasons("lastName",
                    "Invalid last name. (from 2 to 30 Latin letters)"));
        }

        String middleName = request.getMiddleName();
        boolean checkMiddleName =
                middleName.matches("^[a-zA-Z]{2,30}");
        if (!checkMiddleName) {
            reasons.add(new ExceptionReasons("middleName",
                    "Invalid middle name. (from 2 to 30 Latin letters)"));
        }

        BigDecimal requestAmount = request.getAmount();
        int resultCompareTo = requestAmount.compareTo(prescoringConfiguration.getMinRequestedAmount());
        boolean checkAmount = resultCompareTo >= 0;
        if (!checkAmount) {
            reasons.add(new ExceptionReasons("amount",
                    "Invalid amount. (a real number greater than or equal to 10000)"));
        }

        Integer term = request.getTerm();
        boolean checkTerm = term >= prescoringConfiguration.getMinTerm();
        if (!checkTerm) {
            reasons.add(new ExceptionReasons("term",
                    "Invalid term. (an integer greater than or equal to 6)"));
        }

        LocalDate birthDate = request.getBirthdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        boolean checkAge = age >= prescoringConfiguration.getMinAge();
        if (!checkAge) {
            reasons.add(new ExceptionReasons("birthdate",
                    "Invalid birthdate. (in the format yyyy-mm-dd, no later than 18 years from the current day)"));
        }

        String email = request.getEmail();
        boolean checkEmail = email.matches("[\\w\\.]{2,50}@[\\w\\.]{2,20}");
        if (!checkEmail) {
            reasons.add(new ExceptionReasons("email",
                    "Invalid email."));
        }

        String passportSeries = request.getPassportSeries();
        boolean checkPassportSeries = passportSeries.matches("[0-9]{4}");
        if (!checkPassportSeries) {
            reasons.add(new ExceptionReasons("passportSeries",
                    "Invalid passport series. (4 digits)"));
        }

        String passportNumber = request.getPassportNumber();
        boolean checkPassportNumber = passportNumber.matches("[0-9]{6}");
        if (!checkPassportNumber) {
            reasons.add(new ExceptionReasons("passportNumber",
                    "Invalid passport number. (6 digits)"));
        }
        if (!reasons.isEmpty()) throw new PrescoringException(reasons);
    }

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

        /*
        Расчёт коэффициента аннуитета по формуле:
        Месячная ставка * (1 + Месячная ставка)^Кол-во платежей / ((1 + Месячная ставка)^Кол-во платежей - 1)
         */
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12 * 100), 20, RoundingMode.HALF_EVEN);
        BigDecimal bracketsInFormula = monthlyRate.add(BigDecimal.valueOf(1)).pow(term);
        // Числитель формулы
        BigDecimal numeratorAnnuityRatioCalculation = monthlyRate.multiply(bracketsInFormula);
        // Знаменатель формулы
        BigDecimal denominatorAnnuityRatioCalculation = bracketsInFormula.subtract(BigDecimal.valueOf(1));
        BigDecimal annuityRatio =
                numeratorAnnuityRatioCalculation.divide(denominatorAnnuityRatioCalculation, 20, RoundingMode.HALF_EVEN);

        /*
        Расчёт месячного аннуитетного платежа по формуле:
        Сумма кредита * Коэффициент аннуитета
         */
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

    public @NotNull ResponseEntity<List<LoanOfferDTO>> getLoanOffers(@NotNull LoanApplicationRequestDTO request) {
        long applicationId = basicConfiguration.getApplicationId();
        List<LoanOfferDTO> loanOfferDTOList = new ArrayList<>();

        prescoring(request);

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

    private void scoring(ScoringDataDTO request) {
        EmploymentDTO employment = request.getEmployment();

        List<ExceptionReasons> reasons = new ArrayList<>();

        if (employment.getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            reasons.add(new ExceptionReasons("employmentStatus",
                    "You're employment status is UNEMPLOYED."));
        }

        // limitRequestedAmount = salary * 20.0
        BigDecimal limitRequestedAmount = employment.getSalary().multiply(scoringConfiguration.getLoanToSalaryRatio());
        if (limitRequestedAmount.compareTo(request.getAmount()) < 0) {
            reasons.add(new ExceptionReasons("amount/salary",
                    "The loan amount is more than 20 salaries."));
        }

        LocalDate birthDate = request.getBirthdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        if (age < scoringConfiguration.getMinAge() || age > scoringConfiguration.getMaxAge()) {
            reasons.add(new ExceptionReasons("birthdate",
                    "Age less than 20 or more than 60 years."));
        }

        int workExperienceTotal = employment.getWorkExperienceTotal();
        if (workExperienceTotal < scoringConfiguration.getMinWorkExperienceTotal()) {
            reasons.add(new ExceptionReasons("workExperienceTotal",
                    "Total experience less than 12 months."));
        }

        int workExperienceCurrent = employment.getWorkExperienceCurrent();
        if (workExperienceCurrent <= scoringConfiguration.getMinWorkExperienceCurrent()) {
            reasons.add(new ExceptionReasons("workExperienceCurrent",
                    "Current experience less than 3 months."));
        }
        if (!reasons.isEmpty()) throw new ScoringException(reasons);
    }

    /**
     * Метод расчёта графика платежей
     * @link <a href="https://journal.tinkoff.ru/guide/credit-payment/">Как посчитать ежемесячный платеж по кредиту</a>
     * Входные данные:
     * @param amount сумма полученного кредита
     * @param rate ставка по кредиту рассчитанная в методе calculationLoanParams
     * @param term срок выплачивания кредита
     * @param monthlyPayment сумма масячного аннуитетного платежа
     * @param dateFirstPayment дата первого платежа по кредиту(спустя месяц после получения кредита)
     * @return график платежей, состоящий из номера, даты и общей суммы платежа, доли погашения процентов и доли погашения долга от суммы, остатка долга
     */
    private List<PaymentScheduleElement> paymentScheduleCalculation(BigDecimal amount,
                                                                    BigDecimal rate,
                                                                    Integer term,
                                                                    BigDecimal monthlyPayment,
                                                                    LocalDate dateFirstPayment) {


        List<PaymentScheduleElement> paymentSchedule = new ArrayList<>();

        LocalDate datePayment = dateFirstPayment;
        BigDecimal remainingDebt = amount;

        for (int i = 0; i < term; i++) {

            // Количество дней, за прошедший месяц (нужно для вычисления днейвной ставки)
            int lengthMonth = datePayment.minusMonths(1).lengthOfMonth();


            BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 20, RoundingMode.HALF_EVEN);

            // Дневной коэффициент из формулы: Количество дней в месяце / Количество дней в году
            BigDecimal dailyCoef;

            // Проверка на переход в новый год (год, возможно, был/будет високосным => изменится дневная ставка)
            if (datePayment.getMonthValue() == 1 && i > 0) {
                boolean isLeapBeforeYear = datePayment.minusYears(1).isLeapYear();
                int lengthBeforeYear = (isLeapBeforeYear) ? 366: 365;
                boolean isLeapNowYear = datePayment.isLeapYear();
                int lengthNowYear = (isLeapNowYear) ? 366: 365;

                LocalDate monthBefore = datePayment.minusMonths(1);
                // Количество дней от последнего платежа до окончания года
                int daysUntilNextMonth = monthBefore.until(datePayment.withDayOfMonth(1)).getDays() - 1;
                // Количество дней от начала нового года до следующего платежа
                int daysInNowMonth = datePayment.withDayOfMonth(1).until(datePayment).getDays() + 1;

                // Расчёт дневного коэффициента прошлого и нового года
                BigDecimal dailyCoefBeforeYear =
                        BigDecimal.valueOf(daysUntilNextMonth).divide(BigDecimal.valueOf(lengthBeforeYear), 20, RoundingMode.HALF_EVEN);
                BigDecimal dailyCoefNowYear =
                        BigDecimal.valueOf(daysInNowMonth).divide(BigDecimal.valueOf(lengthNowYear), 20, RoundingMode.HALF_EVEN);
                dailyCoef = dailyCoefBeforeYear.add(dailyCoefNowYear);
            }
            else {
                boolean isLeap = datePayment.isLeapYear();
                int lengthYear = (isLeap) ? 366: 365;
                dailyCoef = BigDecimal.valueOf(lengthMonth).divide(BigDecimal.valueOf(lengthYear), 20, RoundingMode.HALF_EVEN);
            }

            /*
            Вычисление суммы процентов по формуле:
            Остаток долга × Процентная ставка × Количество дней в месяце / Количество дней в году
             */
            BigDecimal interestPayment =
                    remainingDebt.multiply(rateDecimal).multiply(dailyCoef).setScale(2, RoundingMode.HALF_EVEN);

            if (i == term - 1) {        /* проверка на последний платёж */
                monthlyPayment = remainingDebt.add(interestPayment);        /* последний платёж состоит из остатка долга + проценты */
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

    /**
     * Метод создания CreditDTO.
     * @link <a href="https://journal.tinkoff.ru/guide/credit-payment/">Как посчитать ежемесячный платеж по кредиту</a>
     * @param amount сумма кредита
     * @param term срок выплачивания кредита
     * @param rate ставка по кредиту рассчитанная в методе calculationLoanParams
     * @param isInsuranceEnabled булево значение включения страховки в сумму кредита
     * @param isSalaryClient булево значение является ли клиент зарплатным
     * @return составляется кредитное предложение с графиком платежей
     */
    private CreditDTO createCreditDTO(BigDecimal amount,
                                      Integer term,
                                      BigDecimal rate,
                                      Boolean isInsuranceEnabled,
                                      Boolean isSalaryClient) {

        /*
        Расчёт коэффициента аннуитета по формуле:
        Месячная ставка * (1 + Месячная ставка)^Кол-во платежей / ((1 + Месячная ставка)^Кол-во платежей - 1)
         */
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12 * 100), 20, RoundingMode.HALF_EVEN);
        BigDecimal bracketsInFormula = monthlyRate.add(BigDecimal.valueOf(1)).pow(term);
        // Числитель формулы
        BigDecimal numeratorAnnuityRatioCalculation = monthlyRate.multiply(bracketsInFormula);
        // Знаменатель формулы
        BigDecimal denominatorAnnuityRatioCalculation = bracketsInFormula.subtract(BigDecimal.valueOf(1));
        BigDecimal annuityRatio = numeratorAnnuityRatioCalculation.divide(denominatorAnnuityRatioCalculation, 20, RoundingMode.HALF_EVEN);

        /*
        Расчёт месячного аннуитетного платежа по формуле:
        Сумма кредита * Коэффициент аннуитета
         */
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

    public @NotNull ResponseEntity<CreditDTO> calculationLoanParams(@NotNull ScoringDataDTO request) {

        BigDecimal defaultRate = basicConfiguration.getDefaultRate();

        scoring(request);

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