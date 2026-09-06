package com.example.World.Threads;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Status;
import com.example.World.Comments.CommentLikeRepository;
import com.example.World.Comments.CommentRepository;
import com.example.World.Follows.FollowService;
import com.example.World.Follows.Follow_;
import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationService;
import com.example.World.Users.UserRepository;
import com.example.World.Users.UserView;
import com.example.World.Users.User_;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final BetRepository betRepository;
    private final ThreadLikeRepository threadLikeRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final FollowService followService;

    ThreadService(ThreadRepository threadRepository, BetRepository betRepository, ThreadLikeRepository threadLikeRepository,
                  UserRepository userRepository, CommentRepository commentRepository, NotificationService notificationService, FollowService followService){

        this.threadRepository = threadRepository;
        this.betRepository = betRepository;
        this.threadLikeRepository = threadLikeRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.followService = followService;
    }

    public Thread_ makeThread(ThreadDTO thread , Long uid){

        Thread_ newThread = threadRepository.save(new Thread_(null,uid,thread.title(), thread.media(), thread.media_type(), thread.category(),0L,
                new Date().getTime(),null,thread.is_private(),null));

        for(Follow_ f : followService.getFollowers(uid)) {

            User_ follower = userRepository.findById(f.request_id()).orElseThrow();

            if(followService.getFollow(uid, follower.uid()) == null && thread.is_private()){
                continue;
            }

            //System.out.println(f.request_id());

            // get friends and loop

            NotificationDTO temp = new NotificationDTO(uid, "new_thread", newThread.tid(), "thread");


            notificationService.registerNotification(follower.fb_notification_token(),
                    thread.media_type() == 1 ? "Photo was posted" : thread.media_type() == 2 ? "Video was posted" : thread.title(),
                    temp, f.request_id());


        }

        return newThread;


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

    private boolean isThreadLike(Long uid , Long tid){
        return threadLikeRepository.findByThreadAndUser(tid,uid).isPresent();
    }

    public List<Threadlike_> getUserThreadLikes(Long uid){
        return threadLikeRepository.findByUser(uid);
    }

    public ThreadProfile toThreadProfile(Long tid){
        Thread_ t = threadRepository.findById(tid).orElseThrow();
        User_ user = userRepository.findById(t.uid()).orElseThrow();

        return new ThreadProfile(t.tid(),UserView.from(user),t.title(),t.media(),t.media_type(),t.category(),t.likes()
                ,isThreadLike(user.uid(),t.tid()),(long) commentRepository.findByThread(t.tid()).size(),t.created_at(), t.is_private());
    }

    public List<ThreadProfile> threadProfileList(Long uid){

        List<ThreadProfile> result = new ArrayList<>();
        List<Thread_> threads = threadRepository.findAllActiveThreads();


        for(Thread_ t : threads){
            User_ user = userRepository.findById(t.uid()).orElseThrow();
            if((followService.getFollow(uid,user.uid()) == null || followService.getFollow(user.uid(),uid) == null) && t.is_private()
            && !t.uid().equals(uid)){
                continue;
            }
            result.add(new ThreadProfile(t.tid(),UserView.from(user),t.title(),t.media(),t.media_type(),t.category(),t.likes()
                    ,isThreadLike(uid,t.tid()),(long) commentRepository.findByThread(t.tid()).size(),t.created_at(), t.is_private()));
        }


        return result;
    }

    public List<ThreadProfile> threadProfileList(Long user_uid,Long target_uid){

        List<ThreadProfile> result = new ArrayList<>();
        List<Thread_> threads = threadRepository.findAllUserThreads(target_uid);
        User_ user = userRepository.findById(target_uid).orElseThrow();



        for(Thread_ t : threads){
            if((followService.getFollow(user_uid,target_uid) == null || followService.getFollow(target_uid,user_uid) == null) && t.is_private()
                    && !t.uid().equals(user_uid)){
                continue;
            }
            result.add(new ThreadProfile(t.tid(),UserView.from(user),t.title(),t.media(),t.media_type(),t.category(),t.likes(),
                    isThreadLike(user_uid,t.tid()),(long) commentRepository.findByThread(t.tid()).size(), t.created_at(),t.is_private()));
        }


        return result;
    }


    public void registerThreadLike(Long uid , Long tid , boolean liked){
        Optional<Threadlike_> tl = threadLikeRepository.findByThreadAndUser(tid,uid);
        if(tl.isEmpty()){
            if(liked) {
                threadLikeRepository.save(new Threadlike_(null, tid, uid));
                threadRepository.increment(tid);

                Long toID = threadRepository.findById(tid).orElseThrow().uid();

                if(toID.equals(uid)){return;}

                User_ threadOwner = userRepository.findById(toID).orElseThrow();


                NotificationDTO temp = new NotificationDTO(uid, "thread_like", tid, "thread");


                notificationService.registerNotification(threadOwner.fb_notification_token(), "Go check it out!" ,
                        temp, toID);
            }
        }else{
            if(!liked){
                threadLikeRepository.delete(tl.get());
                threadRepository.decrement(tid);
            }
        }

    }
}
