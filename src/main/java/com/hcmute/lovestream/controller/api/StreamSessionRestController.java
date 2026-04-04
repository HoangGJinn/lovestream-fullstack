package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.StreamSessionRequest;
import com.hcmute.lovestream.dto.response.StreamSessionResponse;
import com.hcmute.lovestream.service.device.DeviceAccessService;
import com.hcmute.lovestream.service.stream.StreamSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/streams")
@RequiredArgsConstructor
public class StreamSessionRestController {

    private final StreamSessionService streamSessionService;
    private final DeviceAccessService deviceAccessService;

    @PostMapping("/start")
    public ResponseEntity<StreamSessionResponse> start(@Valid @RequestBody StreamSessionRequest request,
                                                       Principal principal,
                                                       HttpServletRequest httpRequest) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            deviceAccessService.touchDevice(principal.getName(), request.getDeviceId(), httpRequest.getHeader("User-Agent"), false);
            StreamSessionResponse response = streamSessionService.start(principal.getName(), request.getDeviceId());
            if (!response.allowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new StreamSessionResponse(
                    false, 0, 0, ex.getMessage()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new StreamSessionResponse(
                    false, 0, 0, "Không thể cập nhật trạng thái phiên xem lúc này."
            ));
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<StreamSessionResponse> heartbeat(@Valid @RequestBody StreamSessionRequest request,
                                                           Principal principal,
                                                           HttpServletRequest httpRequest) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            deviceAccessService.touchDevice(principal.getName(), request.getDeviceId(), httpRequest.getHeader("User-Agent"), false);
            StreamSessionResponse response = streamSessionService.heartbeat(principal.getName(), request.getDeviceId());
            if (!response.allowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new StreamSessionResponse(
                    false, 0, 0, ex.getMessage()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new StreamSessionResponse(
                    false, 0, 0, "Không thể cập nhật trạng thái phiên xem lúc này."
            ));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<StreamSessionResponse> stop(@Valid @RequestBody StreamSessionRequest request,
                                                      Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StreamSessionResponse response = streamSessionService.stop(principal.getName(), request.getDeviceId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new StreamSessionResponse(
                    false, 0, 0, ex.getMessage()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new StreamSessionResponse(
                    false, 0, 0, "Không thể cập nhật trạng thái phiên xem lúc này."
            ));
        }
    }
}
