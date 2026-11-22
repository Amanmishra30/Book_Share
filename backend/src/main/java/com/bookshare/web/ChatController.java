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
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;

    @GetMapping("/request/{id}/messages")
    public List<Dto.MessageDto> requestMessages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return chat.requestHistory(me.id(), id); }

    @PostMapping("/request/{id}/messages")
    public Dto.MessageDto sendRequest(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.SendReq req) {
        return chat.sendRequest(me.id(), id, req.content());
    }

    @GetMapping("/sale/{id}/messages")
    public List<Dto.MessageDto> saleMessages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @RequestParam Long buyerId) {
        return chat.saleHistory(me.id(), id, buyerId);
    }

    @PostMapping("/sale/{id}/messages")
    public Dto.MessageDto sendSale(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.SendReq req) {
        return chat.sendSale(me.id(), id, req.threadUserId(), req.content());
    }

    @GetMapping("/room/{id}/messages")
    public List<Dto.MessageDto> roomMessages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return chat.roomHistory(me.id(), id); }

    @PostMapping("/room/{id}/messages")
    public Dto.MessageDto sendRoom(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.SendReq req) {
        return chat.sendRoom(me.id(), id, req.content());
    }

    @GetMapping("/reading/{id}/messages")
    public List<Dto.MessageDto> readingMessages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return chat.readingHistory(me.id(), id); }

    @PostMapping("/reading/{id}/messages")
    public Dto.MessageDto sendReading(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.SendReq req) {
        return chat.sendReading(me.id(), id, req.content());
    }
}
