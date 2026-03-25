package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.request.CreateRoomRequest;
import com.hcmute.lovestream.dto.response.WatchRoomCardResponse;
import com.hcmute.lovestream.dto.response.WatchRoomStateResponse;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/watch-together")
@RequiredArgsConstructor
public class WatchTogetherWebController {

    private final WatchTogetherService watchTogetherService;

    @GetMapping({"", "/room-list"})
    public String roomList(
            @RequestParam(name = "roomCode", required = false) String roomCode,
            Model model
    ) {
        List<WatchRoomCardResponse> roomCards = watchTogetherService.listPublicRooms();
        model.addAttribute("rooms", roomCards);
        model.addAttribute("roomCodeQuery", roomCode == null ? "" : roomCode.trim().toUpperCase());

        watchTogetherService.findRoomByCode(roomCode).ifPresent(room -> model.addAttribute("searchedRoom", room));
        return "watch-together/room-list";
    }

    @GetMapping("/creat-room")
    public String createRoomPage(Model model) {
        if (!model.containsAttribute("createRoomForm")) {
            model.addAttribute("createRoomForm", new CreateRoomRequest());
        }
        model.addAttribute("movieOptions", watchTogetherService.getCreateRoomMovieOptions());
        return "watch-together/creat-room";
    }

    @PostMapping("/creat-room")
    public String createRoom(
            @Valid @ModelAttribute("createRoomForm") CreateRoomRequest createRoomForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movieOptions", watchTogetherService.getCreateRoomMovieOptions());
            return "watch-together/creat-room";
        }

        try {
            Room room = watchTogetherService.createRoom(authentication.getName(), createRoomForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tao phong thanh cong. Ma phong: " + room.getRoomCode());
            redirectAttributes.addFlashAttribute("showCreatedRoomInfo", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("createdRoomCode", room.getRoomCode());
            redirectAttributes.addFlashAttribute("createdRoomPassword", room.getPassword());
            return "redirect:/watch-together";
        } catch (RuntimeException ex) {
            model.addAttribute("movieOptions", watchTogetherService.getCreateRoomMovieOptions());
            model.addAttribute("errorMessage", ex.getMessage());
            return "watch-together/creat-room";
        }
    }

    @PostMapping("/{roomCode}/join")
    public String joinRoom(
            @PathVariable String roomCode,
            @RequestParam(name = "password", required = false) String password,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Room room = watchTogetherService.joinRoom(roomCode, authentication.getName(), password);
            redirectAttributes.addFlashAttribute("successMessage", "Ban da tham gia phong " + room.getRoomName());
            return "redirect:/watch-movie?id=" + room.getVideoContent().getId() + "&roomCode=" + room.getRoomCode();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/watch-together";
        }
    }

    @GetMapping("/my-rooms")
    public String myRooms(Authentication authentication, Model model) {
        model.addAttribute("myRooms", watchTogetherService.listRoomsHostedBy(authentication.getName()));
        return "watch-together/my-rooms";
    }

    @PostMapping("/{roomCode}/start")
    public String startRoom(
            @PathVariable String roomCode,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Room room = watchTogetherService.startRoom(roomCode, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Phong " + room.getRoomName() + " da bat dau");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/watch-together/my-rooms";
    }

    @PostMapping("/{roomCode}/stop")
    public String stopRoom(
            @PathVariable String roomCode,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Room room = watchTogetherService.stopRoom(roomCode, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Phong " + room.getRoomName() + " da dung");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/watch-together/my-rooms";
    }

    @GetMapping("/api/rooms/{roomCode}/state")
    @ResponseBody
    public ResponseEntity<WatchRoomStateResponse> roomState(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        WatchRoomStateResponse state = watchTogetherService.getRoomState(roomCode, authentication.getName());
        return ResponseEntity.ok(state);
    }

    @PostMapping("/api/rooms/{roomCode}/start")
    @ResponseBody
    public ResponseEntity<?> startRoomApi(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        try {
            watchTogetherService.startRoom(roomCode, authentication.getName());
            return ResponseEntity.ok(watchTogetherService.getRoomState(roomCode, authentication.getName()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/api/rooms/{roomCode}/stop")
    @ResponseBody
    public ResponseEntity<?> stopRoomApi(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        try {
            watchTogetherService.stopRoom(roomCode, authentication.getName());
            return ResponseEntity.ok(watchTogetherService.getRoomState(roomCode, authentication.getName()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}


