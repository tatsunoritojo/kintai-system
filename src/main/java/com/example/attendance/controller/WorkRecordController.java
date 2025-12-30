package com.example.attendance.controller;

import com.example.attendance.dto.WorkRecordDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 勤務記録管理コントローラー.
 */
@Controller
@RequestMapping("/work-records")
public class WorkRecordController {

    @GetMapping
    public String list(Model model) {
        List<WorkRecordDto> workRecords = createMockWorkRecords();
        model.addAttribute("workRecords", workRecords);
        return "work-records/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("workRecord", new WorkRecordDto());
        model.addAttribute("isNew", true);
        return "work-records/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        WorkRecordDto workRecord = createMockWorkRecords().stream()
                .filter(w -> w.getId().equals(id))
                .findFirst()
                .orElse(createMockWorkRecords().get(0));
        model.addAttribute("workRecord", workRecord);
        model.addAttribute("isNew", false);
        return "work-records/form";
    }

    /**
     * 勤務記録保存（プロトタイプ用スタブ）.
     */
    @PostMapping
    public String save(@ModelAttribute WorkRecordDto workRecord, RedirectAttributes redirectAttributes) {
        // プロトタイプ用: 実際には保存処理を実装
        redirectAttributes.addFlashAttribute("message", "勤務記録を保存しました（プロトタイプ）");
        return "redirect:/work-records";
    }

    private List<WorkRecordDto> createMockWorkRecords() {
        return Arrays.asList(
                WorkRecordDto.builder()
                        .id("wr-001")
                        .employeeId("emp-001")
                        .employeeName("田中 太郎")
                        .workDate(LocalDate.of(2024, 1, 15))
                        .startTime(LocalDateTime.of(2024, 1, 15, 9, 0))
                        .endTime(LocalDateTime.of(2024, 1, 15, 18, 0))
                        .workHours(8.0)
                        .workTypeName("通常勤務")
                        .note("特になし")
                        .createdAt(LocalDateTime.of(2024, 1, 15, 18, 5))
                        .build(),
                WorkRecordDto.builder()
                        .id("wr-002")
                        .employeeId("emp-001")
                        .employeeName("田中 太郎")
                        .workDate(LocalDate.of(2024, 1, 16))
                        .startTime(LocalDateTime.of(2024, 1, 16, 9, 0))
                        .endTime(LocalDateTime.of(2024, 1, 16, 20, 0))
                        .workHours(10.0)
                        .workTypeName("通常勤務")
                        .note("残業対応")
                        .createdAt(LocalDateTime.of(2024, 1, 16, 20, 5))
                        .build(),
                WorkRecordDto.builder()
                        .id("wr-003")
                        .employeeId("emp-002")
                        .employeeName("佐藤 花子")
                        .workDate(LocalDate.of(2024, 1, 15))
                        .startTime(LocalDateTime.of(2024, 1, 15, 10, 0))
                        .endTime(LocalDateTime.of(2024, 1, 15, 16, 0))
                        .workHours(5.0)
                        .workTypeName("時短勤務")
                        .note("子育て対応")
                        .createdAt(LocalDateTime.of(2024, 1, 15, 16, 5))
                        .build(),
                WorkRecordDto.builder()
                        .id("wr-004")
                        .employeeId("emp-003")
                        .employeeName("鈴木 一郎")
                        .workDate(LocalDate.of(2024, 1, 15))
                        .startTime(LocalDateTime.of(2024, 1, 15, 22, 0))
                        .endTime(LocalDateTime.of(2024, 1, 16, 6, 0))
                        .workHours(8.0)
                        .workTypeName("夜勤")
                        .note("夜勤シフト")
                        .createdAt(LocalDateTime.of(2024, 1, 16, 6, 5))
                        .build());
    }
}
