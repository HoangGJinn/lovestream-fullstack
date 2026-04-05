package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.service.admin.statistic.AdminStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatisticService statisticService;

    @GetMapping("/admin")
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        Map<String, Object> stats = statisticService.getDashboardStatistics();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }
}
