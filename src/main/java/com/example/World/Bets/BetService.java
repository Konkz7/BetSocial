package com.example.World.Bets;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BetService {
    private final BetRepository betRepository;

    public BetService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void closeExpiredBets() {
        List<Bet_> activeBets = betRepository.findByStatus(Status.ACTIVE.toInt());

        List<Bet_> expiredBets = activeBets.stream()
                .filter(bet -> bet.ends_at().isBefore(LocalDateTime.now()))
                .toList();

        System.out.println("Expired bets: " + expiredBets.size());

        for (Bet_ bet : expiredBets) {
            betRepository.updateStatus(bet.bid(), Status.PENDING.toInt()); // Mark bet as closed
        }
    }
}
