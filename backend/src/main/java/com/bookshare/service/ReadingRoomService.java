package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.MessageRepository;
import com.bookshare.repo.ReadingRoomRepository;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Scheduled shared reading sessions open to every signed-in user: a time slot, a capacity, text chat and optional WebRTC. */
@Service
@RequiredArgsConstructor
public class ReadingRoomService {
    private final ReadingRoomRepository rooms;
    private final MessageRepository msgs;
    private final UserService users;
    private final ChatAccessService access;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public List<Dto.ReadingRoomDto> list(Long meId) {
        return rooms.findAllByOrderByScheduledTimeAsc().stream().map(mapper::reading).toList();
    }

    @Transactional
    public Dto.ReadingRoomDto create(Long meId, Dto.ReadingRoomReq req) {
        User me = users.require(meId);
        ReadingRoom r = new ReadingRoom();
        r.setHost(me);
        r.setTitle(req.title().trim());
        r.setDescription(req.description());
        r.setScheduledTime(req.scheduledTime());
        r.setMaxParticipants(req.maxParticipants());
        r.getParticipants().add(me);
        return mapper.reading(rooms.save(r));
    }

    private ReadingRoom load(Long meId, Long id) {
        access.requireReading(meId, id);
        return rooms.findById(id).orElseThrow(() -> ApiException.notFound("Reading room not found"));
    }

    @Transactional(readOnly = true)
    public Dto.ReadingRoomDto get(Long meId, Long id) { return mapper.reading(load(meId, id)); }

    @Transactional
    public Dto.ReadingRoomDto join(Long meId, Long id) {
        ReadingRoom r = load(meId, id);
        User me = users.require(meId);
        boolean already = r.getParticipants().stream().anyMatch(u -> u.getId().equals(meId));
        if (!already) {
            if (r.getParticipants().size() >= r.getMaxParticipants()) throw ApiException.conflict("This reading room is full.");
            r.getParticipants().add(me);
            rooms.save(r);
        }
        return mapper.reading(r);
    }

    @Transactional
    public Dto.ReadingRoomDto leave(Long meId, Long id) {
        ReadingRoom r = load(meId, id);
        r.getParticipants().removeIf(u -> u.getId().equals(meId));
        rooms.save(r);
        return mapper.reading(r);
    }

    @Transactional
    public void delete(Long meId, Long id) {
        ReadingRoom r = load(meId, id);
        if (!r.getHost().getId().equals(meId)) throw ApiException.forbidden("Only the host can close this reading room.");
        msgs.deleteByReadingRoomId(id);   // chat messages reference the room, so remove them first
        rooms.delete(r);
    }
}
