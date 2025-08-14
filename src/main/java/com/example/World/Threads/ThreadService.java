package com.example.World.Threads;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Status;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Optional;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final BetRepository betRepository;

    ThreadService(ThreadRepository threadRepository, BetRepository betRepository){

        this.threadRepository = threadRepository;
        this.betRepository = betRepository;
    }

    public void removeThread(Long tid, Long uid){
        Optional<Thread_> optionalThread = threadRepository.findById(tid);
        Thread_ thread;
        if(optionalThread.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }else{
            thread = optionalThread.get();
        }

        if(!thread.uid().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this thread");
        }


        threadRepository.remove(tid, new Date().getTime());

        betRepository.findByThread(tid).forEach(bet -> {
            betRepository.updateStatus(bet.bid(), Status.CANCELLED.toInt());
            betRepository.remove(bet.bid(), new Date().getTime());
        });
    }
}
