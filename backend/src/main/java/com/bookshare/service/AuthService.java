package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.security.JwtService;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final CommunityRepository communities;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;

    @Transactional
    public Dto.AuthResponse register(Dto.RegisterReq req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("That email is already registered.");
        Community c = communities.findById(req.communityId()).orElseThrow(() -> ApiException.badRequest("Please choose a community."));
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setCommunity(c);
        u = users.save(u);
        return new Dto.AuthResponse(jwt.generate(u), mapper.userDto(u));
    }

    @Transactional(readOnly = true)
    public Dto.AuthResponse login(Dto.LoginReq req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Wrong email or password."));
        if (u.getAccountStatus() == AccountStatus.SUSPENDED) throw ApiException.forbidden("Your account is suspended.");
        return new Dto.AuthResponse(jwt.generate(u), mapper.userDto(u));
    }
}
