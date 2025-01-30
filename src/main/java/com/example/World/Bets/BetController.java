package com.example.World.Bets;


import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.User_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequestMapping("/api/bets")
@RestController
public class BetController {
    private final BetRepository betRepository;
    private final ThreadRepository threadRepository;

    public BetController(BetRepository betRepository, ThreadRepository threadRepository) {
        this.betRepository = betRepository;
        this.threadRepository = threadRepository;
    }

    @GetMapping("/all")
    List<Bet_> findAll(){
        return betRepository.findAll();
    }

    @GetMapping("/{bid}")
    Bet_ findById(@PathVariable Long bid){
        Optional<Bet_> bet = betRepository.findById(bid);
        if(bet.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "bet not found");
        }
        return bet.get();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Bet_ bet){
        betRepository.save(bet);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    void makeBet(@Valid @RequestBody BetDTO bet) {



        betRepository.save(new Bet_(null, bet.tid(), Status.statusToInt(Status.ACTIVE), null, 0f,0f, bet.description(),
                LocalDateTime.now(), null, LocalDateTime.now().plusSeconds(bet.secondsEndsAt())/*bet.ends_at()*/, null));

    }

    @PostMapping("/decide")
    void decideOutcome(@Valid @RequestBody DecisionDTO decision, HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        Optional<Bet_> optionalBet = betRepository.findById(decision.bid());
        Bet_ bet;
        if(optionalBet.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "bet not found");
        }else{
            bet = optionalBet.get();
        }

        if(!(bet.status() == Status.statusToInt(Status.PENDING) && bet.outcome() == null)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bet cannot be decided at this time");
        }

        threadRepository.findById(bet.tid()).ifPresentOrElse(thread -> {
            if (!userId.equals( thread.uid())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this bet");
            }
        }, () -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet has no valid thread");
        });



        betRepository.updateOutcome(decision.bid(), decision.decision());
        betRepository.makeDecision( decision.bid(), decision.reason(), decision.decision(), LocalDateTime.now(), userId );
    }
/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{bid}")
    void update(@Valid @RequestBody Bet_ bet, @PathVariable Integer bid){
        betRepository.updateBet(bid, bet.outcome(), bet.amount(), bet.status());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{bid}")
    void delete(@PathVariable Long bid){
        betRepository.delete(betRepository.findById(bid).get());
    }
}
