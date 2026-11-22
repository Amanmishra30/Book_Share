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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final UserService users;

    @PostMapping("/register")
    public Dto.AuthResponse register(@Valid @RequestBody Dto.RegisterReq req) { return auth.register(req); }

    @PostMapping("/login")
    public Dto.AuthResponse login(@Valid @RequestBody Dto.LoginReq req) { return auth.login(req); }

    @GetMapping("/me")
    public Dto.UserDto me(@AuthenticationPrincipal AuthUser me) { return users.me(me.id()); }
}
