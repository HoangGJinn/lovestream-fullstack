package com.hcmute.lovestream.dto.response;

import com.hcmute.lovestream.entity.ServicePlan;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ServicePlanResponse {

    private final String id;
    private final String name;
    private final BigDecimal price;
    private final String resolution;
    private final int maxScreens;
    private final int durationDays;
    private final String description;

    public ServicePlanResponse(ServicePlan plan) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.price = plan.getPrice();
        this.resolution = plan.getResolution();
        this.maxScreens = plan.getMaxScreens();
        this.durationDays = plan.getDurationDays();
        this.description = plan.getDescription();
    }

    // Hiển thị thời hạn dạng "1 tháng", "3 tháng", ...
    public String getDurationLabel() {
        if (durationDays <= 0) return "Không xác định";
        int months = durationDays / 30;
        int days   = durationDays % 30;
        if (months > 0 && days == 0) return months + " tháng";
        if (months > 0) return months + " tháng " + days + " ngày";
        return durationDays + " ngày";
    }

    // Hiển thị giá định dạng có dấu phẩy, VD: "99.000 ₫"
    public String getFormattedPrice() {
        if (price == null) return "Miễn phí";
        long amount = price.longValue();
        if (amount == 0) return "Miễn phí";
        return String.format("%,d ₫", amount).replace(',', '.');
    }
}
