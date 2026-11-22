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
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communities;

    /** Public so the sign-up page can list communities before login. */
    @GetMapping("/public")
    public List<Dto.CommunityDto> publicList() { return communities.listAll(); }

    @GetMapping
    public List<Dto.CommunityDto> list() { return communities.listAll(); }

    @PostMapping
    public Dto.CommunityDto create(@Valid @RequestBody Dto.CommunityReq req) { return communities.create(req); }
}
