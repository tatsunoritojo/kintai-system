package com.example.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * バッチ実行管理コントローラー（管理者専用）.
 */
@Controller
@RequestMapping("/admin/batch")
public class BatchController {

    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> batchLogs = createMockBatchLogs();
        model.addAttribute("batchLogs", batchLogs);
        model.addAttribute("lastCalendarSync", "2024-01-15 10:00:00");
        model.addAttribute("lastPayrollCalc", "2024-01-15 06:00:00");
        return "admin/batch";
    }

    @PostMapping("/sync-calendar")
    public String syncCalendar(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Googleカレンダー同期を開始しました（プロトタイプ）");
        return "redirect:/admin/batch";
    }

    @PostMapping("/calculate-payroll")
    public String calculatePayroll(@RequestParam String targetMonth, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", targetMonth + "の給与計算を開始しました（プロトタイプ）");
        return "redirect:/admin/batch";
    }

    @PostMapping("/sync-spreadsheet")
    public String syncSpreadsheet(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Googleスプレッドシート同期を開始しました（プロトタイプ）");
        return "redirect:/admin/batch";
    }

    private List<Map<String, Object>> createMockBatchLogs() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return Arrays.asList(
                Map.of("id", "log-001", "type", "カレンダー同期", "status", "SUCCESS", "timestamp",
                        LocalDateTime.now().minusHours(2).format(fmt), "message", "5件の勤務記録を同期しました"),
                Map.of("id", "log-002", "type", "給与計算", "status", "SUCCESS", "timestamp",
                        LocalDateTime.now().minusHours(8).format(fmt), "message", "4名分の給与を計算しました"),
                Map.of("id", "log-003", "type", "スプレッドシート同期", "status", "SUCCESS", "timestamp",
                        LocalDateTime.now().minusDays(1).format(fmt), "message", "生徒情報を更新しました"),
                Map.of("id", "log-004", "type", "カレンダー同期", "status", "FAILED", "timestamp",
                        LocalDateTime.now().minusDays(2).format(fmt), "message", "API接続エラー（リトライ成功）"));
    }
}
