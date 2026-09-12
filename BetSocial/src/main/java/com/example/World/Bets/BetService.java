package com.example.World.Bets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class BetService {

    private static final Logger log = LoggerFactory.getLogger(BetService.class);
    private final BetRepository betRepository;

    public BetService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void closeExpiredBets() {
        List<Bet_> activeBets = betRepository.findByStatus(Status.ACTIVE.toInt());
        List<Bet_> expiredBets = activeBets.stream()
                .filter(bet -> bet.ends_at() <= new Date().getTime())
                .toList();

        if (!expiredBets.isEmpty()) {
            // Only when there is something to say. This ran every sixty seconds
            // and printed "Expired bets: 0" almost every time, which is the kind
            // of line that teaches people to stop reading logs.
            log.info("Closing {} expired bets", expiredBets.size());
        }

        for (Bet_ bet : expiredBets) {
            betRepository.updateStatus(bet.bid(), Status.PENDING.toInt()); // Mark bet as closed
        }
    }
}
