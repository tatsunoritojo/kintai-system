package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 給与計算結果DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDto {
    private String employeeId;
    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalWorkHours;
    private Double totalPayment;
    private List<WorkRecordDto> workRecords;
    private PayrollSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollSummary {
        private Integer totalWorkDays;
        private Double regularHours;
        private Double overtimeHours;
        private Double regularPayment;
        private Double overtimePayment;
        private Double totalPayment;
    }
}
