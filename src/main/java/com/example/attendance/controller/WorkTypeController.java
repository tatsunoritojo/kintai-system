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
 * 勤務形態マスタ管理コントローラー.
 */
@Controller
@RequestMapping("/work-types")
public class WorkTypeController {

    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> workTypes = createMockWorkTypes();
        model.addAttribute("workTypes", workTypes);
        return "work-types/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("isNew", true);
        return "work-types/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("isNew", false);
        model.addAttribute("workTypeId", id);
        return "work-types/form";
    }

    @PostMapping
    public String save(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "勤務形態を保存しました（プロトタイプ）");
        return "redirect:/work-types";
    }

    private List<Map<String, Object>> createMockWorkTypes() {
        return Arrays.asList(
                Map.of("id", "wt-001", "name", "個別指導", "description", "1対1または1対2の個別指導", "isActive", true),
                Map.of("id", "wt-002", "name", "グループ授業", "description", "3名以上のグループ授業", "isActive", true),
                Map.of("id", "wt-003", "name", "自習室", "description", "自習室監督業務", "isActive", true),
                Map.of("id", "wt-004", "name", "事務作業", "description", "教材準備・事務処理", "isActive", true),
                Map.of("id", "wt-005", "name", "研修", "description", "社内研修・勉強会", "isActive", false));
    }
}
