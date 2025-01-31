package com.example.World.Users;


import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.DecisionDTO;
import com.example.World.Bets.Status;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequestMapping("/superusers")
@RestController
public class SuperUserController {
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final PredictionRepository predictionRepository;

    public SuperUserController(UserRepository userRepository, BetRepository betRepository,
                               PredictionRepository predictionRepository) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.predictionRepository = predictionRepository;
    }

    @GetMapping("/all")
    List<User_> findAll(){
        return userRepository.findAll();
    }

    @GetMapping("/{uid}")
    User_ findById(@PathVariable Long uid){
        Optional<User_> user = userRepository.findById(uid);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user.get();
    }

    @GetMapping("/bets")
    List<Bet_> findAllBets(){
        return betRepository.findAll();
    }

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

        if(bet.status() != Status.statusToInt(Status.PENDING) || bet.outcome() == null ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bet isn't in the correct state");
        }

        betRepository.makeDecision(  bet.bid(), decision.reason(),decision.decision(), LocalDateTime.now(),userId);

        if(decision.decision()) {

            List<Prediction_> predictions = predictionRepository.findByBid(bet.bid());

            for (Prediction_ prediction : predictions) {
                Float payout, chosen, other;
                chosen = prediction.prediction() ? bet.amount_for() : bet.amount_against();
                other = !prediction.prediction() ? bet.amount_for() : bet.amount_against();


                if (prediction.prediction().equals(bet.outcome())) {
                    payout = (prediction.amount_bet() / chosen) * other * 0.8f;
                } else {
                    payout = -prediction.amount_bet();
                }
                predictionRepository.updateAmountWon(prediction.pid(), payout);
            }

            betRepository.updateStatus(bet.bid(), Status.statusToInt(Status.APPROVED));
        }else{
            betRepository.updateStatus(bet.bid(), Status.statusToInt(Status.REJECTED));
        }

    }

/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{uid}")
    void update(@Valid @RequestBody User user, @PathVariable Integer uid){
        UserRepository.updateUser(uid, User.User(), User.amount());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        userRepository.delete(userRepository.findById(userId).get());
    }
}

