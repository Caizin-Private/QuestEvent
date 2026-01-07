package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class ProgramResponseDTO {
    private Long judgeUserId;
    private Long programId;
    private String programTitle;
    private String programDescription;
    private Department department;
    private Instant startDate;
    private Instant endDate;
    private Integer registrationFee;
    private ProgramStatus status;
    private Long hostUserId; // Just return the ID of the host, not the whole object
}