package com.bookshare.service;

import com.bookshare.domain.User;
import com.bookshare.repo.RatingRepository;
import com.bookshare.repo.UserRepository;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final RatingRepository ratings;
    private final ReputationService reputation;
    private final Mapper mapper;

    public User require(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public Dto.UserDto me(Long id) { return mapper.userDto(require(id)); }

    @Transactional(readOnly = true)
    public Dto.ProfileDto profile(Long viewerId, boolean viewerIsAdmin, Long userId) {
        User u = require(userId);
        double[] a = reputation.averages(userId);
        boolean showStatus = viewerIsAdmin || viewerId.equals(userId);
        var recent = ratings.findByRateeIdOrderByCreatedAtDesc(userId).stream().limit(10).map(mapper::rating).toList();
        return new Dto.ProfileDto(mapper.user(u), u.getBio(), u.getCreatedAt(),
                a[3] == 0 ? 0 : (a[0] + a[1] + a[2]) / 3.0, (long) a[3], a[0], a[1], a[2],
                reputation.compliance(userId), showStatus ? u.getAccountStatus().name() : null, recent);
    }

    @Transactional
    public Dto.UserDto updateBio(Long id, String name, String bio) {
        User u = require(id);
        if (name != null && !name.isBlank()) u.setName(name.trim());
        u.setBio(bio == null ? null : bio.trim());
        return mapper.userDto(users.save(u));
    }
}
