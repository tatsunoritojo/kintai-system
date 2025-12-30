package com.example.attendance.controller;

import com.example.attendance.dto.DashboardDto;
import com.example.attendance.dto.WorkRecordDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * ホーム画面コントローラー.
 */
@Controller
public class HomeController {

        private static final String SESSION_ROLE_KEY = "userRole";
        private static final String ROLE_ADMIN = "ADMIN";
        private static final String ROLE_USER = "USER";

        /**
         * 全コントローラーで共通のModel属性を設定.
         */
        @ModelAttribute
        public void addCommonAttributes(HttpSession session, Model model) {
                String role = getCurrentRole(session);
                model.addAttribute("userRole", role);
                model.addAttribute("isAdmin", ROLE_ADMIN.equals(role));
                model.addAttribute("userName", ROLE_ADMIN.equals(role) ? "管理者" : "田中 太郎");
        }

        @GetMapping("/")
        public String index() {
                return "redirect:/login";
        }

        @GetMapping("/login")
        public String login() {
                return "login";
        }

        @GetMapping("/dashboard")
        public String dashboard(HttpSession session, Model model) {
                String role = getCurrentRole(session);
                DashboardDto dashboard = createMockDashboard(role);
                model.addAttribute("dashboard", dashboard);
                return "dashboard";
        }

        /**
         * ロール切り替え（プロトタイプ用）.
         */
        @GetMapping("/switch-role")
        public String switchRole(HttpSession session) {
                String currentRole = getCurrentRole(session);
                String newRole = ROLE_ADMIN.equals(currentRole) ? ROLE_USER : ROLE_ADMIN;
                session.setAttribute(SESSION_ROLE_KEY, newRole);
                return "redirect:/dashboard";
        }

        private String getCurrentRole(HttpSession session) {
                String role = (String) session.getAttribute(SESSION_ROLE_KEY);
                return role != null ? role : ROLE_ADMIN; // デフォルトはADMIN
        }

        private DashboardDto createMockDashboard(String role) {
                // 最近の勤務記録（モック）
                List<WorkRecordDto> recentWorkRecords = Arrays.asList(
                                WorkRecordDto.builder()
                                                .id("wr-001")
                                                .employeeId("emp-001")
                                                .employeeName("田中 太郎")
                                                .workDate(LocalDate.now().minusDays(2))
                                                .startTime(LocalDateTime.now().minusDays(2).withHour(9).withMinute(0))
                                                .endTime(LocalDateTime.now().minusDays(2).withHour(18).withMinute(0))
                                                .workHours(8.0)
                                                .workTypeName("通常勤務")
                                                .build(),
                                WorkRecordDto.builder()
                                                .id("wr-002")
                                                .employeeId("emp-001")
                                                .employeeName("田中 太郎")
                                                .workDate(LocalDate.now().minusDays(1))
                                                .startTime(LocalDateTime.now().minusDays(1).withHour(9).withMinute(0))
                                                .endTime(LocalDateTime.now().minusDays(1).withHour(19).withMinute(30))
                                                .workHours(9.5)
                                                .workTypeName("通常勤務")
                                                .build(),
                                WorkRecordDto.builder()
                                                .id("wr-003")
                                                .employeeId("emp-001")
                                                .employeeName("田中 太郎")
                                                .workDate(LocalDate.now())
                                                .startTime(LocalDateTime.now().withHour(9).withMinute(0))
                                                .endTime(LocalDateTime.now().withHour(17).withMinute(30))
                                                .workHours(7.5)
                                                .workTypeName("通常勤務")
                                                .build());

                // 通知（モック）
                List<DashboardDto.NotificationDto> notifications = Arrays.asList(
                                DashboardDto.NotificationDto.builder()
                                                .id("notif-001")
                                                .message("給与計算が完了しました（2024年1月分）")
                                                .type("success")
                                                .timestamp("2時間前")
                                                .build(),
                                DashboardDto.NotificationDto.builder()
                                                .id("notif-002")
                                                .message("新しい勤務記録が同期されました")
                                                .type("info")
                                                .timestamp("5時間前")
                                                .build());

                // 月次統計（モック）- ADMINは全員合計、USERは個人
                DashboardDto.MonthlyStats monthlyStats;
                if (ROLE_ADMIN.equals(role)) {
                        monthlyStats = DashboardDto.MonthlyStats.builder()
                                        .totalWorkHours(672.0) // 全従業員合計
                                        .totalWorkDays(84) // 全従業員合計
                                        .estimatedPayment(1008000.0) // 全従業員合計
                                        .month("2024年1月")
                                        .build();
                } else {
                        monthlyStats = DashboardDto.MonthlyStats.builder()
                                        .totalWorkHours(168.5) // 個人
                                        .totalWorkDays(21)
                                        .estimatedPayment(252750.0)
                                        .month("2024年1月")
                                        .build();
                }

                return DashboardDto.builder()
                                .userName(ROLE_ADMIN.equals(role) ? "管理者" : "田中 太郎")
                                .userRole(role)
                                .monthlyStats(monthlyStats)
                                .recentWorkRecords(recentWorkRecords)
                                .notifications(notifications)
                                .employeeCount(ROLE_ADMIN.equals(role) ? 4 : null) // ADMINのみ表示
                                .build();
        }
}
