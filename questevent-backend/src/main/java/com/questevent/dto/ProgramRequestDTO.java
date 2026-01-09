package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProgramRequestDTO {
    private String programTitle;
    private String programDescription;
    private Department department;
    private Instant startDate;
    private Instant endDate;
    private ProgramStatus status;
    private Long judgeUserId;
}