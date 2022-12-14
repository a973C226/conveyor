package com.project.conveyor.model;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Hidden
public class ExceptionReasons {
    private String nameField;
    private String description;
}
