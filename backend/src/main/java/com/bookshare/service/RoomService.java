package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.RoomRepository;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Chatrooms (general / genre rooms) and time-boxed book-club rooms. Open to every signed-in user. */
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository rooms;
    private final UserService users;
    private final ChatAccessService access;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public List<Dto.RoomDto> list(Long meId) {
        return rooms.findAllByOrderByCreatedAtDesc().stream().map(mapper::room).toList();
    }

    @Transactional
    public Dto.RoomDto create(Long meId, Dto.RoomReq req) {
        User me = users.require(meId);
        Room r = new Room();
        r.setCreatedBy(me);
        r.setName(req.name().trim());
        r.setTopic(req.topic() == null || req.topic().isBlank() ? null : req.topic().trim());
        r.setType(req.type() == null ? RoomType.CHAT : req.type());
        r.setScheduledAt(r.getType() == RoomType.BOOK_CLUB ? req.scheduledAt() : null);
        return mapper.room(rooms.save(r));
    }

    @Transactional(readOnly = true)
    public Dto.RoomDto get(Long meId, Long id) {
        access.requireRoom(meId, id);
        return mapper.room(rooms.findById(id).orElseThrow());
    }
}
