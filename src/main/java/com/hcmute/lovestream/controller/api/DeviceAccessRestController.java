package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.DeviceTouchRequest;
import com.hcmute.lovestream.dto.response.DeviceAccessItemResponse;
import com.hcmute.lovestream.service.device.DeviceAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceAccessRestController {

    private final DeviceAccessService deviceAccessService;

    @PostMapping("/touch")
    public ResponseEntity<?> touchDevice(@Valid @RequestBody DeviceTouchRequest request,
                                         Principal principal,
                                         HttpServletRequest httpRequest) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            deviceAccessService.touchDevice(principal.getName(), request.getDeviceId(), httpRequest.getHeader("User-Agent"), true);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> myDevices(@RequestParam(required = false) String deviceId,
                                       @RequestParam(defaultValue = "true") boolean includeStreaming,
                                       Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DeviceAccessItemResponse> devices = deviceAccessService.getDevices(
                    principal.getName(),
                    deviceId,
                    includeStreaming
            );
            return ResponseEntity.ok(Map.of("items", devices));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{deviceId}/logout")
    public ResponseEntity<?> logoutDevice(@PathVariable String deviceId,
                                          @RequestParam(required = false) String currentDeviceId,
                                          Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            deviceAccessService.revokeDevice(principal.getName(), deviceId, currentDeviceId);
            return ResponseEntity.ok(Map.of("message", "Đã đăng xuất thiết bị thành công."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
