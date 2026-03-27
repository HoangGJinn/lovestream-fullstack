package com.hcmute.lovestream.controller.web.contentmanager;

import com.hcmute.lovestream.dto.request.contentmanager.GenreRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.service.contentmanager.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/content-manager/genres")
@RequiredArgsConstructor
@Slf4j
public class ContentManagerGenreWebController {

    private final GenreService genreService;

    @GetMapping
    public String listGenres(@RequestParam(defaultValue = "name-asc") String sort, Model model) {
        model.addAttribute("genres", genreService.getAllGenres(sort));
        model.addAttribute("currentSort", sort);
        return "content-manager/genres/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("genreRequest")) {
            model.addAttribute("genreRequest", new GenreRequest());
        }
        return "content-manager/genres/form";
    }

    @PostMapping
    public String createGenre(@Valid @ModelAttribute("genreRequest") GenreRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "content-manager/genres/form";
        }

        try {
            genreService.createGenre(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thể loại thành công!");
            return "redirect:/content-manager/genres";
        } catch (Exception e) {
            log.error("Lỗi khi thêm thể loại: ", e);
            bindingResult.rejectValue("name", "error.genreRequest", e.getMessage());
            return "content-manager/genres/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Genre genre = genreService.getGenreById(id);
            if (!model.containsAttribute("genreRequest")) {
                GenreRequest request = GenreRequest.builder()
                        .name(genre.getName())
                        .build();
                model.addAttribute("genreRequest", request);
            }
            model.addAttribute("genreId", id);
            return "content-manager/genres/form";
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin thể loại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/genres";
        }
    }

    @PostMapping("/{id}")
    public String updateGenre(@PathVariable String id,
                             @Valid @ModelAttribute("genreRequest") GenreRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genreId", id);
            return "content-manager/genres/form";
        }

        try {
            genreService.updateGenre(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thể loại thành công!");
            return "redirect:/content-manager/genres";
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật thể loại: ", e);
            bindingResult.rejectValue("name", "error.genreRequest", e.getMessage());
            model.addAttribute("genreId", id);
            return "content-manager/genres/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteGenre(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            genreService.deleteGenre(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thể loại thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi xóa thể loại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/content-manager/genres";
    }
}
