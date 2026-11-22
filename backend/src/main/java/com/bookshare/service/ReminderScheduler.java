package com.bookshare.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final BorrowService borrow;

    /** Every 15 minutes: overdue loans become RETURN_DUE and an automatic reminder is posted to the chat. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 900_000)
    public void run() {
        int n = borrow.sweepOverdue();
        if (n > 0) log.info("Sent {} overdue reminder(s)", n);
    }
}
