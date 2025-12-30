package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 従業員DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private String id;
    private String employeeNumber;
    private String name;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
