package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 勤務記録DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkRecordDto {
    private String id;
    private String employeeId;
    private String employeeName;
    private LocalDate workDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double workHours;
    private String workTypeName;
    private String note;
    private LocalDateTime createdAt;
}
