package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Single place that decides who may read or write which chat thread (REST history, send and STOMP subscribe). */
@Service
@RequiredArgsConstructor
public class ChatAccessService {
    private static final Pattern REQ = Pattern.compile("^/topic/request\\.(\\d+)$");
    private static final Pattern SALE = Pattern.compile("^/topic/sale\\.(\\d+)\\.(\\d+)$");
    private static final Pattern ROOM = Pattern.compile("^/topic/room\\.(\\d+)$");
    private static final Pattern READING = Pattern.compile("^/topic/reading\\.(\\d+)(\\.signal)?$");

    private final BorrowRequestRepository reqs;
    private final SaleListingRepository sales;
    private final RoomRepository rooms;
    private final ReadingRoomRepository readings;

    @Transactional(readOnly = true)
    public boolean canSubscribe(Long uid, String dest) {
        if (dest == null) return false;
        Matcher m;
        if ((m = REQ.matcher(dest)).matches()) return canAccessRequest(uid, Long.valueOf(m.group(1)));
        if ((m = SALE.matcher(dest)).matches()) return canAccessSale(uid, Long.valueOf(m.group(1)), Long.valueOf(m.group(2)));
        if ((m = ROOM.matcher(dest)).matches()) return canAccessRoom(uid, Long.valueOf(m.group(1)));
        if ((m = READING.matcher(dest)).matches()) return canAccessReading(uid, Long.valueOf(m.group(1)));
        return false;
    }

    @Transactional(readOnly = true)
    public boolean canAccessRequest(Long uid, Long id) {
        return reqs.findById(id).map(r -> r.getBorrower().getId().equals(uid) || r.getOwner().getId().equals(uid)).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canAccessSale(Long uid, Long saleId, Long buyerId) {
        return sales.findById(saleId).map(s -> s.getSeller().getId().equals(uid) || uid.equals(buyerId)).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canAccessRoom(Long uid, Long id) {
        return rooms.existsById(id);   // chatrooms are open to every signed-in user
    }

    @Transactional(readOnly = true)
    public boolean canAccessReading(Long uid, Long id) {
        return readings.existsById(id);   // reading rooms are open to every signed-in user
    }

    public void requireRequest(Long uid, Long id) {
        if (!canAccessRequest(uid, id)) throw ApiException.forbidden("This conversation belongs to someone else.");
    }
    public void requireSale(Long uid, Long saleId, Long buyerId) {
        if (!canAccessSale(uid, saleId, buyerId)) throw ApiException.forbidden("This conversation belongs to someone else.");
    }
    public void requireRoom(Long uid, Long id) {
        if (!canAccessRoom(uid, id)) throw ApiException.notFound("Room not found");
    }
    public void requireReading(Long uid, Long id) {
        if (!canAccessReading(uid, id)) throw ApiException.notFound("Reading room not found");
    }
}
