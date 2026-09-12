package com.example.World.Reports;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Status;
import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.Comment_;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.Wallet.LedgerReason;
import com.example.World.Wallet.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Reporting, and acting on what gets reported.
 *
 * Taking something down is deliberately a soft delete everywhere. A moderation
 * call made in a hurry on a queue of strangers' posts is exactly the kind of
 * decision that turns out to be wrong, and the whole codebase already prefers
 * deleted_at to DELETE for the same reason.
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final PredictionRepository predictionRepository;
    private final LedgerService ledgerService;

    public ReportService(ReportRepository reportRepository, ThreadRepository threadRepository,
                         CommentRepository commentRepository, UserRepository userRepository,
                         BetRepository betRepository, PredictionRepository predictionRepository,
                         LedgerService ledgerService) {
        this.reportRepository = reportRepository;
        this.threadRepository = threadRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.predictionRepository = predictionRepository;
        this.ledgerService = ledgerService;
    }

    // --- submitting -------------------------------------------------------

    public void submit(Long reporterUid, ReportDTO report) {
        requireTargetExists(report.target_type(), report.target_id());

        if (report.target_type() == ReportTarget.USER && report.target_id().equals(reporterUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report yourself");
        }

        if (reportRepository.alreadyReported(reporterUid, report.target_type().name(),
                report.target_id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reported this");
        }

        reportRepository.save(new Report_(null, reporterUid, report.target_type().name(),
                report.target_id(), report.reason().name(), report.detail(),
                new Date().getTime(), null, null, null));
    }

    private void requireTargetExists(ReportTarget type, Long id) {
        boolean exists = switch (type) {
            case THREAD -> threadRepository.findById(id).isPresent();
            case COMMENT -> commentRepository.findById(id).isPresent();
            case USER -> userRepository.findById(id).isPresent();
        };
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is nothing there to report");
        }
    }

    // --- the queue --------------------------------------------------------

    /** Open reports, each carrying the content it is about. */
    public List<ReportView> openReports() {
        return reportRepository.open().stream().map(this::describe).toList();
    }

    private ReportView describe(Report_ report) {
        String content = "(no longer available)";
        String authorName = "(unknown)";
        Long authorUid = null;
        boolean removed = true;

        switch (ReportTarget.valueOf(report.target_type())) {
            case THREAD -> {
                Optional<Thread_> thread = threadRepository.findById(report.target_id());
                if (thread.isPresent()) {
                    content = thread.get().title();
                    authorUid = thread.get().uid();
                    removed = thread.get().deleted_at() != null;
                }
            }
            case COMMENT -> {
                Optional<Comment_> comment = commentRepository.findById(report.target_id());
                if (comment.isPresent()) {
                    content = comment.get().description();
                    authorUid = comment.get().uid();
                    removed = comment.get().deleted_at() != null;
                }
            }
            case USER -> {
                Optional<User_> user = userRepository.findById(report.target_id());
                if (user.isPresent()) {
                    content = user.get().user_name();
                    authorUid = user.get().uid();
                    removed = user.get().deleted_at() != null;
                }
            }
        }

        if (authorUid != null) {
            authorName = userRepository.findById(authorUid)
                    .map(User_::user_name)
                    .orElse("(deleted account)");
        }

        return new ReportView(report.rid(), report.target_type(), report.target_id(),
                report.reason(), report.detail(), content, authorName, authorUid,
                reportRepository.countFor(report.target_type(), report.target_id()),
                removed, report.created_at());
    }

    // --- deciding ---------------------------------------------------------

    /**
     * Acts on a report.
     *
     * Transactional, and public so that the annotation actually takes effect -
     * removing a thread writes a status, a soft delete and a refund per staker,
     * and half of that is worse than none of it. See TransactionalVisibilityTest.
     */
    @Transactional
    public void decide(Long rid, ReportAction action, Long moderatorUid) {
        Report_ report = reportRepository.findById(rid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (report.resolved_at() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That report is already resolved");
        }

        ReportTarget target = ReportTarget.valueOf(report.target_type());

        if (action == ReportAction.SUSPENDED && target != ReportTarget.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only a report about a person can suspend an account");
        }
        if (action == ReportAction.REMOVED && target == ReportTarget.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use SUSPENDED for a report about a person");
        }

        long now = new Date().getTime();

        switch (action) {
            case REMOVED -> takeDown(target, report.target_id(), now);
            case SUSPENDED -> suspend(report.target_id(), now);
            case DISMISSED -> { /* nothing happens to the content. */ }
        }

        // Everybody who reported the same thing gets the same answer. Ten people
        // reporting one thread is one decision, not ten - and leaving the others
        // open would put the row back in the queue with nothing left to do.
        reportRepository.resolveAllFor(report.target_type(), report.target_id(),
                action.name(), moderatorUid, now);
    }

    private void takeDown(ReportTarget target, Long id, long now) {
        switch (target) {
            case THREAD -> removeThread(id, now);
            case COMMENT -> commentRepository.softDelete(id, now);
            case USER -> throw new IllegalStateException("handled by suspend");
        }
    }

    /**
     * Removes a thread and settles up with anybody who had staked on its bets.
     *
     * Cancelling a bet without giving the coins back would have the moderation
     * queue quietly keep other people's stakes - they took part in good faith and
     * the thread being removed is nothing to do with them. This is the same
     * refund BetController.cancelBet performs, for the same reason.
     */
    private void removeThread(Long tid, long now) {
        Thread_ thread = threadRepository.findById(tid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

        threadRepository.remove(tid, now);

        for (var bet : betRepository.findByThread(tid)) {
            for (Prediction_ prediction : predictionRepository.findByBid(bet.bid())) {
                ledgerService.record(prediction.uid(), prediction.amount_bet(),
                        LedgerReason.STAKE_REFUND, bet.bid(),
                        "Bet removed by a moderator: \"" + bet.description() + "\"");
            }
            betRepository.updateStatus(bet.bid(), Status.CANCELLED.toInt());
            betRepository.remove(bet.bid(), now);
        }
    }

    private void suspend(Long uid, long now) {
        // Their threads go too. A suspended account cannot sign in, but nothing
        // filters a soft-deleted author out of the feed, so leaving the threads
        // would suspend the person and publish their posts.
        threadRepository.findAllUserThreads(uid)
                .forEach(thread -> removeThread(thread.tid(), now));

        userRepository.suspend(uid, now);
    }
}
