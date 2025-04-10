package com.example.World.Bets;


import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.User_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequestMapping("/api/bets")
@RestController
public class BetController {
    private final BetRepository betRepository;
    private final ThreadRepository threadRepository;
    private final BetSaveRepository betSaveRepository;

    public BetController(BetRepository betRepository, ThreadRepository threadRepository, BetSaveRepository betSaveRepository) {
        this.betRepository = betRepository;
        this.threadRepository = threadRepository;
        this.betSaveRepository = betSaveRepository;
    }

    @GetMapping("/all")
    List<Bet_> findAll(){
        return betRepository.findAll();
    }

    @GetMapping("/find-by-thread/{tid}")
    List<Bet_>findByThread(@PathVariable Long tid){
        return betRepository.findByThread(tid);
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
    void makeBet(@Valid @RequestBody BetDTO bet, HttpSession session){

        Long uid = (Long) session.getAttribute("userId");
        threadRepository.findById(bet.tid()).ifPresentOrElse(thread ->{
            if(!thread.uid().equals(uid)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this thread");
            }
        }, () -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        });

        betRepository.save(new Bet_(null, bet.tid(), Status.ACTIVE.toInt(), null, 0f,0f,
                bet.description(), new Date().getTime(), null,bet.ends_at(),bet.is_verified(),bet.king_mode(),
                bet.profit_mode(),bet.max_amount(),bet.min_amount(), null));
    }

    @PostMapping("set-bet")
    void setBet(@RequestParam Long bid, @RequestParam Long uid){
        Betsave_ temp = betSaveRepository.findByBetAndUser(bid,uid);
        if(temp == null){
            betSaveRepository.save(new Betsave_(null,bid,uid));
        }else{
            betSaveRepository.delete(temp);
        }
    }

    @GetMapping("saved")
    Betsave_ getBetSave(@RequestParam Long bid, @RequestParam Long uid){
        return betSaveRepository.findByBetAndUser(bid,uid);
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

        if(!(bet.status() == Status.PENDING.toInt() && bet.outcome() == null)){
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
        betRepository.makeDecision( decision.bid(), decision.reason(), decision.decision(), new Date().getTime(), userId );
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PutMapping("/cancel/{bid}")
    void cancelBet(@PathVariable Long bid , HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        Optional<Bet_> optionalBet = betRepository.findById(bid);
        Bet_ bet;
        if(optionalBet.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "bet not found");
        }else{
            bet = optionalBet.get();
        }

        if(!(bet.status() == Status.PENDING.toInt() && bet.outcome() != null)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bet cannot be cancelled at this time");
        }

        threadRepository.findById(bet.tid()).ifPresentOrElse(thread -> {
            if (!userId.equals( thread.uid())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this bet");
            }
        }, () -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bet has no valid thread");
        });

        betRepository.updateStatus(bid, Status.CANCELLED.toInt());
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
