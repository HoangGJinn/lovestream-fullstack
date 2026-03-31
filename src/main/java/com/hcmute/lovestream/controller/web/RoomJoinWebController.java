package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RoomJoinWebController {

    private final WatchTogetherService watchTogetherService;

    @GetMapping("/join/{roomCode}")
    public String joinRoomByLink(
            @PathVariable String roomCode,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        Room room = watchTogetherService.findRoomEntityByCode(roomCode).orElse(null);
        if (room == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phòng không tồn tại");
            return "redirect:/watch-together";
        }

        if (room.isPrivate() && !watchTogetherService.isUserHost(roomCode, authentication.getName())) {
            model.addAttribute("roomCode", room.getRoomCode());
            model.addAttribute("roomName", room.getRoomName());
            return "watch-together/join-private";
        }

        try {
            Room joinedRoom = watchTogetherService.joinRoom(roomCode, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Ban da tham gia phong " + joinedRoom.getRoomName());
            return "redirect:/watch-movie?id=" + joinedRoom.getVideoContent().getId() + "&roomCode=" + joinedRoom.getRoomCode();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/watch-together";
        }
    }

    @PostMapping("/join/{roomCode}")
    public String joinRoomByLinkWithPassword(
            @PathVariable String roomCode,
            @RequestParam(name = "password", required = false) String password,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        try {
            Room room = watchTogetherService.joinRoom(roomCode, authentication.getName(), password);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã tham gia phòng" + room.getRoomName());
            return "redirect:/watch-movie?id=" + room.getVideoContent().getId() + "&roomCode=" + room.getRoomCode();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/join/" + roomCode;
        }
    }
}


