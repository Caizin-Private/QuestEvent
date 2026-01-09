package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ProgramResponseDTO {
    private Long judgeUserId;
    private Long programId;
    private String programTitle;
    private String programDescription;
    private Department department;
    private Instant startDate;
    private Instant endDate;
    private ProgramStatus status;
    private Long hostUserId;
}