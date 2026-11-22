package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final BorrowRequestRepository reqs;
    private final RatingRepository ratings;
    private final ReputationService reputation;
    private final Mapper mapper;

    /** Two-way rating, once per direction per completed transaction. */
    @Transactional
    public Dto.RatingDto rate(Long meId, Long requestId, Dto.RatingReq req) {
        BorrowRequest r = reqs.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        boolean isBorrower = r.getBorrower().getId().equals(meId);
        boolean isOwner = r.getOwner().getId().equals(meId);
        if (!isBorrower && !isOwner) throw ApiException.forbidden("Only the two people in this exchange can rate it.");
        if (r.getStatus() != BorrowStatus.COMPLETED) throw ApiException.conflict("You can rate once the exchange is completed.");
        if (ratings.existsByBorrowRequestIdAndRaterId(requestId, meId)) throw ApiException.conflict("You already rated this exchange.");
        User rater = isBorrower ? r.getBorrower() : r.getOwner();
        User ratee = isBorrower ? r.getOwner() : r.getBorrower();
        Rating x = new Rating();
        x.setBorrowRequest(r);
        x.setRater(rater);
        x.setRatee(ratee);
        x.setCommunication(req.communication());
        x.setCare(req.care());
        x.setReliability(req.reliability());
        x.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        x = ratings.save(x);
        reputation.recalc(ratee.getId());
        return mapper.rating(x);
    }
}
