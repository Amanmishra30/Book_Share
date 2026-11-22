package com.bookshare.service;

import com.bookshare.domain.Book;
import com.bookshare.repo.BookRepository;
import com.bookshare.repo.BorrowRequestRepository;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** "Readers who borrowed this also borrowed ...": plain co-occurrence over borrow history, no ML. */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final BorrowRequestRepository reqs;
    private final BookRepository books;
    private final BookService bookService;
    private final CommunityService communities;
    private final UserService users;

    private List<Dto.BookDto> resolve(Long meId, LinkedHashMap<Long, Long> ranked, Set<Long> exclude, int limit) {
        Set<Long> visible = new HashSet<>(communities.visibleIds(users.require(meId), "parent"));
        List<Book> out = new ArrayList<>();
        for (Long id : ranked.keySet()) {
            if (exclude.contains(id)) continue;
            books.findById(id).filter(b -> !b.isArchived() && visible.contains(b.getCommunity().getId())
                    && !b.getOwner().getId().equals(meId)).ifPresent(out::add);
            if (out.size() >= limit) break;
        }
        return bookService.dtos(out);
    }

    @Transactional(readOnly = true)
    public List<Dto.BookDto> forBook(Long meId, Long bookId) {
        LinkedHashMap<Long, Long> ranked = new LinkedHashMap<>();
        for (Object[] row : reqs.coBorrowed(bookId)) ranked.put((Long) row[0], ((Number) row[1]).longValue());
        return resolve(meId, ranked, Set.of(bookId), 6);
    }

    @Transactional(readOnly = true)
    public List<Dto.BookDto> forMe(Long meId) {
        List<Long> mine = reqs.borrowedBookIds(meId);
        Map<Long, Long> score = new HashMap<>();
        for (Long b : mine) for (Object[] row : reqs.coBorrowed(b)) score.merge((Long) row[0], ((Number) row[1]).longValue(), Long::sum);
        LinkedHashMap<Long, Long> ranked = new LinkedHashMap<>();
        score.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .forEach(e -> ranked.put(e.getKey(), e.getValue()));
        return resolve(meId, ranked, new HashSet<>(mine), 8);
    }
}
