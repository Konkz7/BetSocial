package com.example.World.Predictions;


import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/predictions")
@RestController
public class PredictionController {
    private final PredictionRepository predictionRepository;
    private final BetRepository betRepository;
    private final ThreadRepository threadRepository;


    public PredictionController(PredictionRepository predictionRepository, BetRepository betRepository, ThreadRepository threadRepository) {
        this.predictionRepository = predictionRepository;
        this.betRepository = betRepository;
        this.threadRepository = threadRepository;
    }

    @GetMapping("/all")
    List<Prediction_> findAll(){
        return predictionRepository.findAll();
    }

    @GetMapping("/{pid}")
    Prediction_ findById(@PathVariable Long pid){
        Optional<Prediction_> prediction = predictionRepository.findById(pid);
        if(prediction.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found");
        }
        return prediction.get();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Prediction_ prediction){
        predictionRepository.save(prediction);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    void makePrediction(@Valid @RequestBody PredictionDTO prediction, HttpSession session){


        Long uid = (Long) session.getAttribute("userId");
        Bet_ bet;

        Optional<Bet_> optionalBet = betRepository.findById(prediction.bid());
        if (optionalBet.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet not found");
        } else {
            bet = optionalBet.get();
        }


        if (bet.status() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bet isnt active anymore");
        } else if(!predictionRepository.findByUidAndBid(uid,prediction.bid()).isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has already made a prediction for this bid");
        }

        threadRepository.findById(bet.tid()).ifPresentOrElse((thread -> {
            if(thread.uid().equals(uid)){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cant make a prediction on their own bet");
                }
            }),
            () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
            });

        predictionRepository.save(new Prediction_(null,prediction.bid(),uid, prediction.prediction(),prediction.amount_bet(),0f,LocalDateTime.now(),null,null));


        if(prediction.prediction()){
            predictionRepository.updateAmountFor(prediction.bid(), prediction.amount_bet());
        }else{
            predictionRepository.updateAmountAgainst(prediction.bid(), prediction.amount_bet());
        }
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PutMapping("/update/{pid}")
    void updatePrediction(@Valid @RequestBody PredictionDTO prediction, @PathVariable Long pid){
        predictionRepository.updatePrediction(pid,prediction.prediction(), prediction.amount_bet());

        predictionRepository.resetPredictionAmount(prediction.bid());

        if(prediction.prediction()){
            predictionRepository.updateAmountFor(prediction.bid(), prediction.amount_bet());
        }else{
            predictionRepository.updateAmountAgainst(prediction.bid(), prediction.amount_bet());
        }
    }






/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{pid}")
    void update(@Valid @RequestBody Prediction_ prediction, @PathVariable Integer pid){
        predictionRepository.updatePrediction(pid, prediction.prediction(), prediction.amount());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{pid}")
    void delete(@PathVariable Long pid){
        predictionRepository.delete(predictionRepository.findById(pid).get());
    }
}

