package com.example.World.Wallet;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping("/api/wallet")
@RestController
public class WalletController {

    private final LedgerService ledgerService;

    public WalletController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * The caller's own wallet. There is no endpoint for anybody else's - a balance
     * is nobody's business but its owner's.
     *
     * Reading it also collects the daily top-up if one is due. That makes a GET
     * change something, which is not free, but the alternatives are worse: a
     * scheduled job grants nothing for the days the server was down, and a claim
     * button turns "you get coins every day" into a chore that punishes anyone who
     * forgets. It is idempotent in the way that matters - looking twice in a day
     * pays once.
     */
    @GetMapping
    WalletDTO wallet(HttpSession session) {
        Long uid = requireUserId(session);

        ledgerService.topUpIfDue(uid);

        return new WalletDTO(ledgerService.balanceOf(uid), ledgerService.historyOf(uid));
    }

    private static Long requireUserId(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }
}
