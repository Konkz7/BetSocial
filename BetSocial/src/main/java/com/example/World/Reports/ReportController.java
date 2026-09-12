package com.example.World.Reports;

import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.RateLimiter;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Submitting a report.
 *
 * There is no endpoint here for reading reports - not even your own. A reporter
 * does not need to see the queue, and publishing whether something has been
 * reported tells whoever posted it that somebody complained.
 */
@RequestMapping("/api/reports")
@RestController
public class ReportController {

    private final ReportService reportService;
    private final RateLimiter rateLimiter;

    public ReportController(ReportService reportService,
                             RateLimiter rateLimiter) {
        this.reportService = reportService;
        this.rateLimiter = rateLimiter;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    void report(@Valid @RequestBody ReportDTO report, HttpSession session) {
        Long uid = requireUserId(session);
        rateLimiter.require(RateLimiter.scopeOf("reports", uid), Limits.REPORTS);
        reportService.submit(uid, report);
    }

    private static Long requireUserId(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }
}
