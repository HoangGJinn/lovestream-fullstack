package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.Vnpay;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vnpay")
@RequiredArgsConstructor
public class VnpayController {

    private final VnpayService vnpayService;

    // POST /api/v1/vnpay/create-payment — Trả về paymentUrl để frontend redirect sang VNPay
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Vnpay vnpayRequest, HttpServletRequest request) {
        try {
            String paymentUrl = vnpayService.createPayment(vnpayRequest, request);
            return ResponseEntity.ok(Map.of(
                    "paymentUrl", paymentUrl
            ));
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

