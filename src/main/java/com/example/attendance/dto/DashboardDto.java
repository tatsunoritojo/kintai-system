package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ダッシュボードDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private String userName;
    private String userRole;
    private MonthlyStats monthlyStats;
    private List<WorkRecordDto> recentWorkRecords;
    private List<NotificationDto> notifications;
    private Integer employeeCount; // ADMIN用：従業員数

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStats {
        private Double totalWorkHours;
        private Integer totalWorkDays;
        private Double estimatedPayment;
        private String month;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationDto {
        private String id;
        private String message;
        private String type;
        private String timestamp;
    }
}
