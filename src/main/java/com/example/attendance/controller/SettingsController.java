package com.example.attendance.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * システム設定コントローラー.
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static final String SESSION_ROLE_KEY = "userRole";
    private static final String ROLE_ADMIN = "ADMIN";

    @GetMapping
    public String settings(HttpSession session, Model model) {
        String role = getCurrentRole(session);
        model.addAttribute("userRole", role);
        model.addAttribute("isAdmin", ROLE_ADMIN.equals(role));
        model.addAttribute("userName", ROLE_ADMIN.equals(role) ? "管理者" : "田中 太郎");
        model.addAttribute("googleCalendarConnected", false);
        model.addAttribute("googleSheetsConnected", false);
        return "settings";
    }

    private String getCurrentRole(HttpSession session) {
        String role = (String) session.getAttribute(SESSION_ROLE_KEY);
        return role != null ? role : ROLE_ADMIN;
    }
}
