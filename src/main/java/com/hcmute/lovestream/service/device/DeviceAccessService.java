package com.hcmute.lovestream.service.device;

import com.hcmute.lovestream.dto.response.DeviceAccessItemResponse;
import com.hcmute.lovestream.entity.Device;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.repository.DeviceRepository;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.stream.StreamSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeviceAccessService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StreamSessionService streamSessionService;

    @Transactional
    public void touchDevice(String userEmail, String rawDeviceId, String userAgent, boolean allowReactivation) {
        String deviceId = normalizeDeviceId(rawDeviceId);
        User user = getUserByEmail(userEmail);
        LocalDateTime now = LocalDateTime.now();

        Device device = deviceRepository.findByUserAndClientDeviceId(user, deviceId)
                .orElseGet(() -> Device.builder()
                        .user(user)
                        .clientDeviceId(deviceId)
                        .build());

        if (!device.isActive() && !allowReactivation) {
            throw new IllegalArgumentException("Thiết bị này đã bị đăng xuất. Vui lòng đăng nhập lại.");
        }

        DeviceInfo info = resolveDeviceInfo(userAgent);
        device.setDeviceName(info.deviceName());
        device.setOs(info.os());
        device.setLastLogin(now);
        device.setActive(true);
        deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceAccessItemResponse> getDevices(String userEmail, String rawCurrentDeviceId) {
        User user = getUserByEmail(userEmail);
        String currentDeviceId = normalizeOptionalDeviceId(rawCurrentDeviceId);
        Set<String> streamingDeviceIds = streamSessionService.getActiveDeviceIds(userEmail);

        return deviceRepository.findByUserOrderByLastLoginDesc(user).stream()
                .map(device -> {
                    String deviceId = device.getClientDeviceId();
                    return new DeviceAccessItemResponse(
                            deviceId,
                            fallback(device.getDeviceName(), "Thiết bị không xác định"),
                            fallback(device.getOs(), "Không rõ hệ điều hành"),
                            device.getLastLogin(),
                            currentDeviceId != null && currentDeviceId.equals(deviceId),
                            device.isActive(),
                            streamingDeviceIds.contains(deviceId)
                    );
                })
                .toList();
    }

    @Transactional
    public void revokeDevice(String userEmail, String rawTargetDeviceId, String rawCurrentDeviceId) {
        User user = getUserByEmail(userEmail);
        String targetDeviceId = normalizeDeviceId(rawTargetDeviceId);
        String currentDeviceId = normalizeOptionalDeviceId(rawCurrentDeviceId);

        if (currentDeviceId != null && currentDeviceId.equals(targetDeviceId)) {
            throw new IllegalArgumentException("Không thể đăng xuất thiết bị hiện tại tại màn hình này.");
        }

        Device device = deviceRepository.findByUserAndClientDeviceId(user, targetDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị cần đăng xuất."));

        device.setActive(false);
        deviceRepository.save(device);
        refreshTokenRepository.revokeUserTokensByDeviceId(user.getId(), targetDeviceId);
        streamSessionService.stop(userEmail, targetDeviceId);
    }

    @Transactional(readOnly = true)
    public boolean isDeviceActive(String userEmail, String rawDeviceId) {
        String deviceId = normalizeOptionalDeviceId(rawDeviceId);
        if (deviceId == null) {
            return false;
        }
        return deviceRepository.findByUser_EmailAndClientDeviceId(userEmail, deviceId)
                .map(Device::isActive)
                .orElse(false);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    private String normalizeDeviceId(String rawDeviceId) {
        String normalized = rawDeviceId == null ? "" : rawDeviceId.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("deviceId không hợp lệ.");
        }
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("deviceId vượt quá độ dài cho phép.");
        }
        return normalized;
    }

    private String normalizeOptionalDeviceId(String rawDeviceId) {
        if (rawDeviceId == null) {
            return null;
        }
        String normalized = rawDeviceId.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 128) {
            return null;
        }
        return normalized;
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private DeviceInfo resolveDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new DeviceInfo("Trình duyệt web", "Không rõ hệ điều hành");
        }

        String ua = userAgent.toLowerCase(Locale.ROOT);
        String os = detectOs(ua);
        String browser = detectBrowser(ua);
        return new DeviceInfo(browser + " - " + os, os);
    }

    private String detectOs(String ua) {
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) return "iOS";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return "Không rõ hệ điều hành";
    }

    private String detectBrowser(String ua) {
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "Trình duyệt web";
    }

    private record DeviceInfo(String deviceName, String os) {}
}
