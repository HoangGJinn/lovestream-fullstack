package com.hcmute.lovestream.controller.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@Slf4j
public class PlanRedirectWebController {

    @GetMapping("/plans")
    public String redirectPlans() {
        log.info("Redirect /plans -> /packages");
        return "redirect:/packages";
    }

    @GetMapping("/plans/{id}")
    public String redirectPlanDetail(@PathVariable String id) {
        log.info("Redirect /plans/{} -> /packages/{}", id, id);
        return "redirect:/packages/" + id;
    }
}

