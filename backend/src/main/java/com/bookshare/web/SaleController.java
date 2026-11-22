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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService sales;

    @GetMapping
    public List<Dto.SaleDto> search(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "own") String scope) {
        return sales.search(me.id(), q, scope);
    }

    @GetMapping("/mine")
    public List<Dto.SaleDto> mine(@AuthenticationPrincipal AuthUser me) { return sales.mine(me.id()); }

    @GetMapping("/{id}")
    public Dto.SaleDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.get(me.id(), id); }

    @PostMapping
    public Dto.SaleDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody Dto.SaleCreateReq req) { return sales.create(me.id(), req.bookId()); }

    @PostMapping("/{id}/interest")
    public Dto.ThreadDto interest(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.interest(me.id(), id); }

    @GetMapping("/{id}/threads")
    public List<Dto.ThreadDto> threads(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.threads(me.id(), id); }

    @PutMapping("/{id}/reserve")
    public Dto.SaleDto reserve(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.ReserveReq req) {
        return sales.reserve(me.id(), id, req);
    }

    @PutMapping("/{id}/release")
    public Dto.SaleDto release(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.release(me.id(), id); }

    @PutMapping("/{id}/sold")
    public Dto.SaleDto sold(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.sold(me.id(), id); }

    /** Demo checkout only: marks the listing sold without collecting real money. */
    @PostMapping("/{id}/simulate-payment")
    public Dto.SaleDto simulatePayment(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return sales.simulatePayment(me.id(), id);
    }

    @PutMapping("/{id}/cancel")
    public Dto.SaleDto cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return sales.cancel(me.id(), id); }
}
