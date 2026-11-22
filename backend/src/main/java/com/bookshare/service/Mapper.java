package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.web.Dto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Entity -> DTO conversion. Call inside a transaction (some associations are lazy). */
@Component
public class Mapper {

    public Dto.UserSummary user(User u) {
        if (u == null) return null;
        Community c = u.getCommunity();
        return new Dto.UserSummary(u.getId(), u.getName(), c == null ? null : c.getId(), c == null ? null : c.getName());
    }

    public Dto.UserDto userDto(User u) {
        Community c = u.getCommunity();
        return new Dto.UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getAccountStatus().name(),
                u.getRestrictedUntil(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                u.getReputationScore(), u.getComplianceRatio());
    }

    public Dto.BookDto book(Book b, SaleListing sale) {
        return new Dto.BookDto(b.getId(), user(b.getOwner()), b.getCommunity().getId(), b.getCommunity().getName(),
                b.getTitle(), b.getAuthor(), b.getGenre(), b.getEdition(), b.getBookCondition().name(), b.getCoverUrl(),
                b.getDescription(), b.getListingType().name(), b.getPrice(), b.getLendingDurationDays(), b.isAvailable(),
                b.getCreatedAt(), sale == null ? null : sale.getId(), sale == null ? null : sale.getStatus().name());
    }

    public Dto.ConditionCheckDto check(ConditionCheck c) {
        return new Dto.ConditionCheckDto(c.getId(), c.getBook().getId(),
                c.getBorrowRequest() == null ? null : c.getBorrowRequest().getId(), c.getStage().name(),
                new ArrayList<>(c.getPhotoUrls()), user(c.getLoggedBy()), c.getCreatedAt(), c.getAiNotes());
    }

    public Dto.MessageDto message(Message m) {
        return new Dto.MessageDto(m.getId(), user(m.getSender()), m.getContent(), m.isSystemMessage(), m.getSentAt(),
                m.getThreadUser() == null ? null : m.getThreadUser().getId());
    }

    public Dto.RatingDto rating(Rating r) {
        return new Dto.RatingDto(r.getId(), r.getBorrowRequest().getId(), user(r.getRater()), user(r.getRatee()),
                r.getCommunication(), r.getCare(), r.getReliability(), r.getComment(), r.getCreatedAt());
    }

    public Dto.ReportDto report(Report r, List<Dto.ConditionCheckDto> checks) {
        BorrowRequest br = r.getBorrowRequest();
        return new Dto.ReportDto(r.getId(), br.getId(), br.getBook().getId(), br.getBook().getTitle(), user(r.getReporter()),
                user(r.getReportedUser()), r.getCategory().name(), r.getStatus().name(), r.getDetails(),
                new ArrayList<>(r.getEvidencePhotoUrls()), r.getAdminNote(), r.getCreatedAt(), r.getResolvedAt(), checks);
    }

    public Dto.SaleDto sale(SaleListing s) {
        return new Dto.SaleDto(s.getId(), book(s.getBook(), s), user(s.getSeller()), user(s.getBuyer()),
                s.getStatus().name(), s.getAgreedPrice(), s.getCreatedAt());
    }

    public Dto.RoomDto room(Room r) {
        return new Dto.RoomDto(r.getId(), r.getName(), r.getTopic(), r.getType().name(), r.getScheduledAt(),
                user(r.getCreatedBy()));
    }

    public Dto.ReadingRoomDto reading(ReadingRoom r) {
        return new Dto.ReadingRoomDto(r.getId(), r.getTitle(), r.getDescription(), r.getScheduledTime(), r.getMaxParticipants(),
                user(r.getHost()), r.getParticipants().stream().map(this::user).toList());
    }
}
