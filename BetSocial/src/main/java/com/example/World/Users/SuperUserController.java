package com.example.World.Users;


import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.DecisionDTO;
import com.example.World.Bets.Status;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import com.example.World.Wallet.LedgerReason;
import com.example.World.Wallet.LedgerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/superusers")
@RestController
public class SuperUserController {

    /**
     * How much of the losing pool is paid out to the winners. The rest is
     * destroyed - there is no house to collect it, and a closed economy that only
     * ever mints coins inflates until the numbers stop meaning anything.
     */
    private static final long PAYOUT_PERCENT = 80L;

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final PredictionRepository predictionRepository;
    private final LedgerService ledgerService;

    public SuperUserController(UserRepository userRepository, BetRepository betRepository,
                               PredictionRepository predictionRepository, LedgerService ledgerService) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.predictionRepository = predictionRepository;
        this.ledgerService = ledgerService;
    }

    @GetMapping("/all")
    List<UserView> findAll(){
        return userRepository.findAll().stream().map(UserView::from).toList();
    }

    @GetMapping("/{uid}")
    UserView findById(@PathVariable Long uid){
        Optional<User_> user = userRepository.findById(uid);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return UserView.from(user.get());
    }

    @GetMapping("/bets")
    List<Bet_> findAllBets(){
        return betRepository.findAll();
    }

    @Transactional
    @PostMapping("/approval")
    void decideApproval(@RequestBody DecisionDTO decision, HttpSession session){
        Optional<Bet_> user_bet = betRepository.findById(decision.bid());
        Bet_ bet;
        Long userId = (Long) session.getAttribute("userId");

        if(user_bet.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet not found");
        }else{
            bet = user_bet.get();
        }

        if(bet.status() != Status.PENDING.toInt() || bet.outcome() == null ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bet isn't in the correct state");
        }

        betRepository.makeDecision(  bet.bid(), decision.reason(),decision.decision(), new Date().getTime(),userId);

        List<Prediction_> predictions = predictionRepository.findByBid(bet.bid());

        if(decision.decision()) {
            settle(bet, predictions);
            betRepository.updateStatus(bet.bid(), Status.APPROVED.toInt());
        }else{
            // The outcome was not accepted, so nothing was decided and nobody
            // should be out of pocket for having taken part.
            refundEveryone(bet, predictions, "Bet rejected: \"" + bet.description() + "\"");
            betRepository.updateStatus(bet.bid(), Status.REJECTED.toInt());
        }

    }

    /**
     * Pays out an approved bet.
     *
     * A winner gets their own stake back plus a share of what the other side
     * staked, in proportion to how much of the winning side they put up. Twenty
     * per cent of the losing pool is not paid out: with virtual coins there is no
     * house to take it, so it simply ceases to exist. That is deliberate - top-ups
     * mint coins every day and something has to remove them, or balances inflate
     * until the numbers stop meaning anything.
     *
     * Losers get nothing back. Their stake left their wallet when they placed it,
     * so there is no debit to make here; the coins are already gone.
     *
     * If nobody backed the winning outcome there is no one to pay, and everybody
     * is refunded rather than the whole table losing its stakes to nobody.
     */
    private void settle(Bet_ bet, List<Prediction_> predictions) {
        long winningPool = bet.outcome() ? bet.amount_for() : bet.amount_against();
        long losingPool  = bet.outcome() ? bet.amount_against() : bet.amount_for();

        // No winners: the bet decided nothing, so nothing changes hands. Also the
        // only way winningPool can be zero while there is somebody to pay, which
        // is what used to make this divide by zero and store an infinite payout.
        if (winningPool <= 0) {
            refundEveryone(bet, predictions,
                    "Nobody predicted the outcome of \"" + bet.description() + "\"");
            return;
        }

        for (Prediction_ prediction : predictions) {
            if (!prediction.prediction().equals(bet.outcome())) {
                // Already paid for, when they placed it.
                predictionRepository.updateAmountWon(prediction.pid(), -prediction.amount_bet());
                continue;
            }

            // Integer arithmetic, and the multiplication comes first so the
            // division is the only place anything is lost. Rounding down means the
            // pool can never pay out more than it holds.
            long winnings = (prediction.amount_bet() * losingPool * PAYOUT_PERCENT)
                    / (winningPool * 100L);

            predictionRepository.updateAmountWon(prediction.pid(), winnings);

            ledgerService.record(prediction.uid(), prediction.amount_bet() + winnings,
                    LedgerReason.WINNINGS, bet.bid(),
                    "Won \"" + bet.description() + "\"");
        }
    }

    /** Gives every stake back, for a bet that resolved to nothing. */
    private void refundEveryone(Bet_ bet, List<Prediction_> predictions, String description) {
        for (Prediction_ prediction : predictions) {
            predictionRepository.updateAmountWon(prediction.pid(), 0L);
            ledgerService.record(prediction.uid(), prediction.amount_bet(),
                    LedgerReason.STAKE_REFUND, bet.bid(), description);
        }
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        userRepository.delete(userRepository.findById(userId).get());
    }
}

