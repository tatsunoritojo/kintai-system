package com.example.attendance.controller;

import com.example.attendance.dto.EmployeeDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 従業員管理コントローラー.
 */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    @GetMapping
    public String list(Model model) {
        List<EmployeeDto> employees = createMockEmployees();
        model.addAttribute("employees", employees);
        return "employees/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        EmployeeDto employee = createMockEmployees().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(createMockEmployees().get(0));
        model.addAttribute("employee", employee);
        return "employees/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new EmployeeDto());
        model.addAttribute("isNew", true);
        return "employees/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        EmployeeDto employee = createMockEmployees().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(createMockEmployees().get(0));
        model.addAttribute("employee", employee);
        model.addAttribute("isNew", false);
        return "employees/form";
    }

    /**
     * 従業員保存（プロトタイプ用スタブ）.
     */
    @PostMapping
    public String save(@ModelAttribute EmployeeDto employee, RedirectAttributes redirectAttributes) {
        // プロトタイプ用: 実際には保存処理を実装
        redirectAttributes.addFlashAttribute("message", "従業員情報を保存しました（プロトタイプ）");
        return "redirect:/employees";
    }

    private List<EmployeeDto> createMockEmployees() {
        return Arrays.asList(
                EmployeeDto.builder()
                        .id("emp-001")
                        .employeeNumber("EMP001")
                        .name("田中 太郎")
                        .email("tanaka@example.com")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now().minusMonths(6))
                        .updatedAt(LocalDateTime.now().minusDays(5))
                        .build(),
                EmployeeDto.builder()
                        .id("emp-002")
                        .employeeNumber("EMP002")
                        .name("佐藤 花子")
                        .email("sato@example.com")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now().minusMonths(3))
                        .updatedAt(LocalDateTime.now().minusDays(2))
                        .build(),
                EmployeeDto.builder()
                        .id("emp-003")
                        .employeeNumber("EMP003")
                        .name("鈴木 一郎")
                        .email("suzuki@example.com")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now().minusMonths(1))
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .build(),
                EmployeeDto.builder()
                        .id("emp-004")
                        .employeeNumber("EMP004")
                        .name("高橋 美咲")
                        .email("takahashi@example.com")
                        .status("INACTIVE")
                        .createdAt(LocalDateTime.now().minusMonths(12))
                        .updatedAt(LocalDateTime.now().minusMonths(2))
                        .build()
        );
    }
}
