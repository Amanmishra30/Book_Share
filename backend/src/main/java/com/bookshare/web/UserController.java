package com.bookshare.web;

import com.bookshare.security.AuthUser;
import com.bookshare.service.*;
import com.bookshare.web.Dto;
import com.bookshare.domain.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    public record ProfileUpdate(String name, String bio) { }

    @GetMapping("/{id}/profile")
    public Dto.ProfileDto profile(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return users.profile(me.id(), me.isAdmin(), id);
    }

    @PutMapping("/me")
    public Dto.UserDto update(@AuthenticationPrincipal AuthUser me, @RequestBody ProfileUpdate body) {
        return users.updateBio(me.id(), body.name(), body.bio());
    }
}
