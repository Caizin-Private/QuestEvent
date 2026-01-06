package com.questevent.dto;

import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProgramRequestDTO {
    private Long hostUserId;
    private String programTitle;
    private String programDescription;
    private Department department;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer registrationFee;
    private ProgramStatus status;
    private Long judgeUserId;
}