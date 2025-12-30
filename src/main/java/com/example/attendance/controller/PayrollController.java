package com.example.attendance.controller;

import com.example.attendance.dto.PayrollDto;
import com.example.attendance.dto.WorkRecordDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 給与計算コントローラー.
 */
@Controller
@RequestMapping("/payrolls")
public class PayrollController {

        @GetMapping
        public String list(Model model) {
                List<PayrollDto> payrolls = createMockPayrolls();
                model.addAttribute("payrolls", payrolls);
                return "payrolls/list";
        }

        @GetMapping("/calculate")
        public String calculateForm(Model model) {
                return "payrolls/calculate";
        }

        /**
         * 給与計算実行（プロトタイプ用スタブ）.
         */
        @PostMapping("/calculate")
        public String calculate(@RequestParam String targetMonth, RedirectAttributes redirectAttributes) {
                // プロトタイプ用: 実際には計算処理を実装
                redirectAttributes.addFlashAttribute("message", targetMonth + "の給与計算を実行しました（プロトタイプ）");
                return "redirect:/payrolls";
        }

        @GetMapping("/{employeeId}")
        public String detail(@PathVariable String employeeId, Model model) {
                PayrollDto payroll = createMockPayroll(employeeId);
                model.addAttribute("payroll", payroll);
                return "payrolls/detail";
        }

        private List<PayrollDto> createMockPayrolls() {
                return Arrays.asList(
                                PayrollDto.builder()
                                                .employeeId("emp-001")
                                                .employeeName("田中 太郎")
                                                .startDate(LocalDate.of(2024, 1, 1))
                                                .endDate(LocalDate.of(2024, 1, 31))
                                                .totalWorkHours(168.0)
                                                .totalPayment(252000.0)
                                                .summary(PayrollDto.PayrollSummary.builder()
                                                                .totalWorkDays(21)
                                                                .regularHours(160.0)
                                                                .overtimeHours(8.0)
                                                                .regularPayment(240000.0)
                                                                .overtimePayment(12000.0)
                                                                .totalPayment(252000.0)
                                                                .build())
                                                .build(),
                                PayrollDto.builder()
                                                .employeeId("emp-002")
                                                .employeeName("佐藤 花子")
                                                .startDate(LocalDate.of(2024, 1, 1))
                                                .endDate(LocalDate.of(2024, 1, 31))
                                                .totalWorkHours(105.0)
                                                .totalPayment(157500.0)
                                                .summary(PayrollDto.PayrollSummary.builder()
                                                                .totalWorkDays(21)
                                                                .regularHours(105.0)
                                                                .overtimeHours(0.0)
                                                                .regularPayment(157500.0)
                                                                .overtimePayment(0.0)
                                                                .totalPayment(157500.0)
                                                                .build())
                                                .build());
        }

        private PayrollDto createMockPayroll(String employeeId) {
                List<WorkRecordDto> workRecords = Arrays.asList(
                                WorkRecordDto.builder()
                                                .id("wr-001")
                                                .workDate(LocalDate.of(2024, 1, 5))
                                                .startTime(LocalDateTime.of(2024, 1, 5, 9, 0))
                                                .endTime(LocalDateTime.of(2024, 1, 5, 18, 0))
                                                .workHours(8.0)
                                                .workTypeName("通常勤務")
                                                .build(),
                                WorkRecordDto.builder()
                                                .id("wr-002")
                                                .workDate(LocalDate.of(2024, 1, 6))
                                                .startTime(LocalDateTime.of(2024, 1, 6, 9, 0))
                                                .endTime(LocalDateTime.of(2024, 1, 6, 18, 0))
                                                .workHours(8.0)
                                                .workTypeName("通常勤務")
                                                .build(),
                                WorkRecordDto.builder()
                                                .id("wr-003")
                                                .workDate(LocalDate.of(2024, 1, 7))
                                                .startTime(LocalDateTime.of(2024, 1, 7, 9, 0))
                                                .endTime(LocalDateTime.of(2024, 1, 7, 20, 0))
                                                .workHours(10.0)
                                                .workTypeName("通常勤務")
                                                .build());

                return PayrollDto.builder()
                                .employeeId(employeeId)
                                .employeeName("田中 太郎")
                                .startDate(LocalDate.of(2024, 1, 1))
                                .endDate(LocalDate.of(2024, 1, 31))
                                .totalWorkHours(168.0)
                                .totalPayment(252000.0)
                                .workRecords(workRecords)
                                .summary(PayrollDto.PayrollSummary.builder()
                                                .totalWorkDays(21)
                                                .regularHours(160.0)
                                                .overtimeHours(8.0)
                                                .regularPayment(240000.0)
                                                .overtimePayment(12000.0)
                                                .totalPayment(252000.0)
                                                .build())
                                .build();
        }
}
