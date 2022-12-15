package com.project.conveyor.controller;

import com.project.conveyor.exception.ExceptionResponse;
import com.project.conveyor.exception.PrescoringException;
import com.project.conveyor.exception.ScoringException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

@ControllerAdvice
public class ExceptionController extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PrescoringException.class)
    public final ResponseEntity<ExceptionResponse> handlePrescoringExceptions(PrescoringException ex) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(
                new Date().toString(),
                ex.getReasons()
        );
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ScoringException.class)
    public final ResponseEntity<ExceptionResponse> handleScoringExceptions(ScoringException ex) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(
                new Date().toString(),
                ex.getReasons()
        );
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

}