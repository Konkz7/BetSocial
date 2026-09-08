package com.example.World.Predictions;


import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Status;
import com.example.World.Wallet.LedgerReason;
import com.example.World.Wallet.LedgerService;
import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/predictions")
@RestController
public class PredictionController {
    private final PredictionRepository predictionRepository;
    private final BetRepository betRepository;
    private final ThreadRepository threadRepository;
    private final LedgerService ledgerService;


    public PredictionController(PredictionRepository predictionRepository, BetRepository betRepository, ThreadRepository threadRepository, LedgerService ledgerService) {
        this.predictionRepository = predictionRepository;
        this.betRepository = betRepository;
        this.threadRepository = threadRepository;
        this.ledgerService = ledgerService;
    }

    // GET /all is deliberately absent. It returned every prediction in the
    // database to any authenticated caller: who backed what, and for how much.
    // That was already more than anyone needed and is now a list of everybody's
    // financial position. It had no client caller.

    /**
     * The caller's own predictions.
     *
     * One request rather than one per bet: a thread can carry several, and the
     * screen needs to know which of them this person has already staked on -
     * a prediction cannot be changed once placed, so the difference decides
     * whether a bet is still open to them.
     */
    @GetMapping("/mine")
    List<Prediction_> mine(HttpSession session){
        return predictionRepository.findByUid(requireUserId(session));
    }

    /**
     * A single prediction, readable only by whoever made it.
     *
     * This used to return anybody's by id, which meant walking pids to read what
     * everyone had staked and on which side.
     */
    @GetMapping("/{pid}")
    Prediction_ findById(@PathVariable Long pid, HttpSession session){
        Prediction_ prediction = predictionRepository.findById(pid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));

        if (!prediction.uid().equals(requireUserId(session))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That is not your prediction");
        }

        return prediction;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @PostMapping("/make")
    void makePrediction(@Valid @RequestBody PredictionDTO prediction, HttpSession session){


        Long uid = requireUserId(session);
        Bet_ bet;

        Optional<Bet_> optionalBet = betRepository.findById(prediction.bid());
        if (optionalBet.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet not found");
        } else {
            bet = optionalBet.get();
        }


        if (bet.status() != Status.ACTIVE.toInt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bet isnt active anymore");
        }

        // The scheduled sweep only runs every minute, so a bet can be past its
        // closing time and still marked active. Staking is refused on the time
        // rather than on the status.
        if (bet.ends_at() <= new Date().getTime()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This bet has closed");
        }

        threadRepository.findById(bet.tid()).ifPresentOrElse((thread -> {
            if(thread.uid().equals(uid)){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cant make a prediction on their own bet");
                }
            }),
            () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
            });

        long stake = prediction.amount_bet();
        requireWithinLimits(bet, stake);

        // A prediction cannot be changed or withdrawn once it is placed, so a
        // second one on the same bet is refused rather than replacing the first.
        // See mayAmend for what this is deliberately leaving room for.
        predictionRepository.findByUidAndBid(uid, prediction.bid()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already predicted on this bet, and a stake cannot be changed once placed");
        });

        // Checked before the stake is taken, and inside the transaction, so a
        // balance cannot be spent twice by two requests arriving together.
        ledgerService.requireBalance(uid, stake);

        predictionRepository.save(new Prediction_(null, prediction.bid(), uid,
                prediction.prediction(), stake, null, new Date().getTime(), null, null));

        if(prediction.prediction()){
            predictionRepository.updateAmountFor(prediction.bid(), stake);
        }else{
            predictionRepository.updateAmountAgainst(prediction.bid(), stake);
        }

        // Taken now, not at settlement. Nothing was deducted before, so a user
        // could stake far more than they held and the pools were make-believe.
        ledgerService.record(uid, -stake, LedgerReason.STAKE, bet.bid(),
                "Stake on \"" + bet.description() + "\"");
    }

    /**
     * Whether a prediction that has already been placed may still be altered.
     *
     * Always no, for now: a stake is committed when it is made. The alternative -
     * letting people move until the bet closes - makes watching the pool and
     * jumping to the popular side at the last moment both free and strictly better
     * than deciding early, which leaves predicting well worth very little.
     *
     * The intended middle ground is to allow changes up to some window before
     * ends_at, so that late switching is blocked while an early mistake can still
     * be undone. That is a change to this one method plus the refund and re-stake
     * paths it would need; the decision lives here so it only has to be made once.
     */
    private static boolean mayAmend(Bet_ bet, long now) {
        return false;
    }

    /**
     * A stake has to be worth something and has to sit inside whatever limits the
     * bet was created with. Those limits were checked in the client and nowhere
     * else, so a request that did not come from the client ignored them entirely.
     */
    private static void requireWithinLimits(Bet_ bet, long stake) {
        if (stake <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A stake has to be at least one coin");
        }
        if (bet.min_amount() > 0 && stake < bet.min_amount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This bet takes at least " + bet.min_amount() + " coins");
        }
        // Zero means no ceiling, which is how the client has always read it.
        if (bet.max_amount() > 0 && stake > bet.max_amount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This bet takes at most " + bet.max_amount() + " coins");
        }
    }

    private static Long requireUserId(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(uid == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }

    // updateBetPool is gone with the ability to change a prediction. It moved a
    // stake between the two pools when somebody switched sides, which cannot
    // happen now. If changes come back it will be needed again - and will need to
    // move coins as well as pool totals, which it never did.


    /**
     * Withdrawing a prediction. Refused, because a stake is committed when it is
     * placed.
     *
     * Kept as an endpoint that says so rather than deleted, because the reason is
     * worth stating: it used to take the stake back out of the pool and soft-delete
     * the prediction, which cost nothing at all. Somebody could watch which way a
     * bet was going and pull out whenever it turned against them, so a losing
     * position was only ever a temporary condition.
     *
     * Coins are only ever given back for something outside the predictor's control
     * - the bet being cancelled or rejected, or settling with nobody on the
     * winning side. If mayAmend ever returns true this becomes a real withdrawal
     * again, and has to refund the stake through the ledger as well as unwind the
     * pool.
     */
    @PutMapping("/remove/{pid}")
    void removePrediction(@PathVariable Long pid , HttpSession session){
        Long userId = requireUserId(session);

        Prediction_ prediction = predictionRepository.findById(pid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));

        if(!prediction.uid().equals(userId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this prediction");
        }

        Bet_ bet = betRepository.findById(prediction.bid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet not found"));

        if (!mayAmend(bet, new Date().getTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A stake cannot be withdrawn once it has been placed");
        }

        if(prediction.prediction()){
            predictionRepository.updateAmountFor(prediction.bid(), -prediction.amount_bet());
        }else{
            predictionRepository.updateAmountAgainst(prediction.bid(), -prediction.amount_bet());
        }

        predictionRepository.remove(pid, new Date().getTime());
    }






}

