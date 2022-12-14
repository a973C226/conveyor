package com.project.conveyor.exception;

import com.project.conveyor.model.ExceptionReasons;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
public class ExceptionResponse {
    private String timestamp;
    private List<ExceptionReasons> details;
}
