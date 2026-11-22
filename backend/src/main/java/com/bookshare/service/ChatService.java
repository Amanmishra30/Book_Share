package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Persists chat messages and pushes them live to STOMP subscribers on /topic/... */
@Service
@RequiredArgsConstructor
public class ChatService {
    private final MessageRepository msgs;
    private final BorrowRequestRepository reqs;
    private final SaleListingRepository sales;
    private final RoomRepository rooms;
    private final ReadingRoomRepository readings;
    private final UserRepository users;
    private final ChatAccessService access;
    private final Mapper mapper;
    private final SimpMessagingTemplate ws;

    private Dto.MessageDto publish(Message m, String destination) {
        Dto.MessageDto dto = mapper.message(msgs.save(m));
        ws.convertAndSend(destination, dto);
        return dto;
    }

    private User user(Long id) { return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found")); }

    private static String clean(String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw ApiException.badRequest("Message is empty.");
        return c;
    }

    // ---------- borrow request threads ----------
    @Transactional(readOnly = true)
    public List<Dto.MessageDto> requestHistory(Long me, Long id) {
        access.requireRequest(me, id);
        return msgs.findByBorrowRequestIdOrderBySentAtAsc(id).stream().map(mapper::message).toList();
    }

    @Transactional
    public Dto.MessageDto sendRequest(Long me, Long id, String content) {
        access.requireRequest(me, id);
        Message m = new Message();
        m.setBorrowRequest(reqs.getReferenceById(id));
        m.setSender(user(me));
        m.setContent(clean(content));
        return publish(m, "/topic/request." + id);
    }

    @Transactional
    public void systemRequest(BorrowRequest r, String text) {
        Message m = new Message();
        m.setBorrowRequest(r);
        m.setContent(text);
        m.setSystemMessage(true);
        publish(m, "/topic/request." + r.getId());
    }

    // ---------- sale listing threads (one thread per interested buyer) ----------
    @Transactional(readOnly = true)
    public List<Dto.MessageDto> saleHistory(Long me, Long saleId, Long buyerId) {
        access.requireSale(me, saleId, buyerId);
        return msgs.findBySaleListingIdAndThreadUserIdOrderBySentAtAsc(saleId, buyerId).stream().map(mapper::message).toList();
    }

    @Transactional
    public Dto.MessageDto sendSale(Long me, Long saleId, Long threadUserId, String content) {
        SaleListing s = sales.findById(saleId).orElseThrow(() -> ApiException.notFound("Listing not found"));
        if (s.getStatus() != SaleStatus.LISTED && s.getStatus() != SaleStatus.RESERVED) {
            throw ApiException.conflict("This listing is closed.");
        }
        boolean isSeller = s.getSeller().getId().equals(me);
        Long buyerId = isSeller ? threadUserId : me;
        if (buyerId == null || buyerId.equals(s.getSeller().getId())) throw ApiException.badRequest("Choose which buyer you are replying to.");
        access.requireSale(me, saleId, buyerId);
        if (isSeller && !msgs.existsBySaleListingIdAndThreadUserId(saleId, buyerId)) {
            throw ApiException.badRequest("That buyer has not started a chat yet.");
        }
        Message m = new Message();
        m.setSaleListing(s);
        m.setThreadUser(user(buyerId));
        m.setSender(user(me));
        m.setContent(clean(content));
        return publish(m, "/topic/sale." + saleId + "." + buyerId);
    }

    @Transactional
    public void systemSale(SaleListing s, Long buyerId, String text) {
        Message m = new Message();
        m.setSaleListing(s);
        m.setThreadUser(user(buyerId));
        m.setContent(text);
        m.setSystemMessage(true);
        publish(m, "/topic/sale." + s.getId() + "." + buyerId);
    }

    // ---------- community chatrooms / book clubs ----------
    @Transactional(readOnly = true)
    public List<Dto.MessageDto> roomHistory(Long me, Long id) {
        access.requireRoom(me, id);
        return msgs.findByRoomIdOrderBySentAtAsc(id).stream().map(mapper::message).toList();
    }

    @Transactional
    public Dto.MessageDto sendRoom(Long me, Long id, String content) {
        access.requireRoom(me, id);
        Message m = new Message();
        m.setRoom(rooms.getReferenceById(id));
        m.setSender(user(me));
        m.setContent(clean(content));
        return publish(m, "/topic/room." + id);
    }

    // ---------- reading rooms (text chat; audio/video signalling goes through SignalingController) ----------
    @Transactional(readOnly = true)
    public List<Dto.MessageDto> readingHistory(Long me, Long id) {
        access.requireReading(me, id);
        return msgs.findByReadingRoomIdOrderBySentAtAsc(id).stream().map(mapper::message).toList();
    }

    @Transactional
    public Dto.MessageDto sendReading(Long me, Long id, String content) {
        access.requireReading(me, id);
        Message m = new Message();
        m.setReadingRoom(readings.getReferenceById(id));
        m.setSender(user(me));
        m.setContent(clean(content));
        return publish(m, "/topic/reading." + id);
    }
}
