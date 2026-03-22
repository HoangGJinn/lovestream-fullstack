package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ServicePlanWebController {

    private final ServicePlanService servicePlanService;

    // GET /packages — Danh sách tất cả gói dịch vụ đang active
    @GetMapping("/packages")
    public String getAllPackages(Model model) {
        log.info("GET /packages");
        List<ServicePlanResponse> plans = servicePlanService.getAllActivePlans();
        model.addAttribute("plans", plans);

        return "plans/list";
    }

    // GET /packages/{id} — Chi tiết 1 gói
    @GetMapping("/packages/{id}")
    public String getPackageDetail(@PathVariable String id, Model model) {
        log.info("GET /packages/{}", id);
        try {
            ServicePlanResponse plan = servicePlanService.getPlanById(id);
            model.addAttribute("plan", plan);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }

        return "plans/detail";
    }
}
