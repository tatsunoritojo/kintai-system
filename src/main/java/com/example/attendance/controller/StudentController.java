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
 * 生徒マスタ管理コントローラー.
 */
@Controller
@RequestMapping("/students")
public class StudentController {

    @GetMapping
    public String list(Model model) {
        List<Map<String, Object>> students = createMockStudents();
        model.addAttribute("students", students);
        return "students/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("isNew", true);
        return "students/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("isNew", false);
        model.addAttribute("studentId", id);
        return "students/form";
    }

    @PostMapping
    public String save(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "生徒情報を保存しました（プロトタイプ）");
        return "redirect:/students";
    }

    private List<Map<String, Object>> createMockStudents() {
        return Arrays.asList(
                Map.of("id", "stu-001", "name", "山田 花子", "level", "中学3年", "school", "第一中学校", "status", "ACTIVE"),
                Map.of("id", "stu-002", "name", "佐藤 健太", "level", "高校2年", "school", "県立高校", "status", "ACTIVE"),
                Map.of("id", "stu-003", "name", "鈴木 美咲", "level", "中学1年", "school", "第二中学校", "status", "ACTIVE"),
                Map.of("id", "stu-004", "name", "高橋 翔太", "level", "高校3年", "school", "私立高校", "status", "INACTIVE"));
    }
}
