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
@RequiredArgsConstructor
public class RoomController {
    private final RoomService rooms;
    private final ReadingRoomService reading;

    @GetMapping("/api/rooms")
    public List<Dto.RoomDto> rooms(@AuthenticationPrincipal AuthUser me) { return rooms.list(me.id()); }

    @PostMapping("/api/rooms")
    public Dto.RoomDto createRoom(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody Dto.RoomReq req) { return rooms.create(me.id(), req); }

    @GetMapping("/api/rooms/{id}")
    public Dto.RoomDto room(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return rooms.get(me.id(), id); }

    @GetMapping("/api/reading-rooms")
    public List<Dto.ReadingRoomDto> readingRooms(@AuthenticationPrincipal AuthUser me) { return reading.list(me.id()); }

    @PostMapping("/api/reading-rooms")
    public Dto.ReadingRoomDto createReading(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody Dto.ReadingRoomReq req) { return reading.create(me.id(), req); }

    @GetMapping("/api/reading-rooms/{id}")
    public Dto.ReadingRoomDto readingRoom(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return reading.get(me.id(), id); }

    @PostMapping("/api/reading-rooms/{id}/join")
    public Dto.ReadingRoomDto join(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return reading.join(me.id(), id); }

    @PostMapping("/api/reading-rooms/{id}/leave")
    public Dto.ReadingRoomDto leave(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return reading.leave(me.id(), id); }

    @DeleteMapping("/api/reading-rooms/{id}")
    public ResponseEntity<Void> close(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        reading.delete(me.id(), id);
        return ResponseEntity.noContent().build();
    }
}
