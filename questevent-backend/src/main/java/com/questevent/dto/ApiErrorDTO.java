package com.questevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorDTO {

    private int status;
    private String message;
    private Instant timestamp;

}
