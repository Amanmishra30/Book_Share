package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.CommunityRepository;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityRepository repo;

    /** Community ids a user may search: own | parent (the whole parent subtree) | root (the whole top-level tree). */
    @Transactional(readOnly = true)
    public List<Long> visibleIds(User u, String scope) {
        Community own = u.getCommunity();
        if (scope == null || scope.isBlank() || scope.equals("own")) return List.of(own.getId());
        Community anchor = own;
        if (scope.equals("parent")) {
            if (own.getParent() != null) anchor = own.getParent();
        } else if (scope.equals("root")) {
            while (anchor.getParent() != null) anchor = anchor.getParent();
        } else {
            throw ApiException.badRequest("scope must be own, parent or root");
        }
        Map<Long, List<Long>> children = new HashMap<>();
        for (Community c : repo.findAll()) {
            if (c.getParent() != null) children.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c.getId());
        }
        List<Long> out = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>(List.of(anchor.getId()));
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            out.add(id);
            stack.addAll(children.getOrDefault(id, List.of()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Dto.CommunityDto> listAll() {
        return repo.findAll().stream()
                .map(this::dto)
                .sorted(Comparator.comparing(Dto.CommunityDto::path))
                .toList();
    }

    @Transactional
    public Dto.CommunityDto create(Dto.CommunityReq req) {
        Community c = new Community();
        c.setName(req.name().trim());
        c.setLevel(req.level());
        if (req.parentId() != null) {
            c.setParent(repo.findById(req.parentId()).orElseThrow(() -> ApiException.notFound("Parent community not found")));
        }
        return dto(repo.save(c));
    }

    public Dto.CommunityDto dto(Community c) {
        return new Dto.CommunityDto(c.getId(), c.getName(), c.getLevel().name(),
                c.getParent() == null ? null : c.getParent().getId(), path(c));
    }

    private String path(Community c) {
        List<String> names = new ArrayList<>();
        for (Community x = c; x != null; x = x.getParent()) names.add(x.getName());
        Collections.reverse(names);
        return String.join(" \u203A ", names);
    }
}
