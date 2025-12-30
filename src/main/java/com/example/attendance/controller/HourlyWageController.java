package com.example.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 単価マスタ管理コントローラー.
 */
@Controller
@RequestMapping("/hourly-wages")
public class HourlyWageController {

    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> wages = createMockWages();
        model.addAttribute("wages", wages);
        return "hourly-wages/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("isNew", true);
        return "hourly-wages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("isNew", false);
        model.addAttribute("wageId", id);
        return "hourly-wages/form";
    }

    @PostMapping
    public String save(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "単価情報を保存しました（プロトタイプ）");
        return "redirect:/hourly-wages";
    }

    private List<Map<String, Object>> createMockWages() {
        return Arrays.asList(
                Map.of("id", "wage-001", "workType", "個別指導", "studentLevel", "中学生", "wage", 3000),
                Map.of("id", "wage-002", "workType", "個別指導", "studentLevel", "高校生", "wage", 3500),
                Map.of("id", "wage-003", "workType", "自習室", "studentLevel", "-", "wage", 1200),
                Map.of("id", "wage-004", "workType", "グループ授業", "studentLevel", "中学生", "wage", 2500),
                Map.of("id", "wage-005", "workType", "グループ授業", "studentLevel", "高校生", "wage", 2800));
    }
}
