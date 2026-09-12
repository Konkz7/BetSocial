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

import com.example.World.Blocks.BlockService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final BetRepository betRepository;
    private final ThreadLikeRepository threadLikeRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final FollowService followService;
    private final BlockService blockService;

    ThreadService(ThreadRepository threadRepository, BetRepository betRepository, ThreadLikeRepository threadLikeRepository,
                  UserRepository userRepository, CommentRepository commentRepository, NotificationService notificationService, FollowService followService,
                  BlockService blockService){

        this.threadRepository = threadRepository;
        this.betRepository = betRepository;
        this.threadLikeRepository = threadLikeRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.followService = followService;
        this.blockService = blockService;
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

    /**
     * @param viewerUid the user asking. The liked flag previously used the thread
     *                  author's id, so this endpoint reported whether the author
     *                  had liked their own thread rather than whether the caller
     *                  had - the list variants below already used the viewer.
     */
    public ThreadProfile toThreadProfile(Long tid, Long viewerUid){
        Thread_ t = threadRepository.findById(tid).orElseThrow();
        User_ user = userRepository.findById(t.uid()).orElseThrow();

        // The feed filters blocked authors out, but this reads a thread by id and
        // would otherwise hand it over to anyone holding the number.
        blockService.requireNotBlocked(viewerUid, t.uid());

        return new ThreadProfile(t.tid(),UserView.from(user),t.title(),t.media(),t.media_type(),t.category(),t.likes()
                ,isThreadLike(viewerUid,t.tid()),(long) commentRepository.findByThread(t.tid()).size(),t.created_at(), t.is_private());
    }

    public List<ThreadProfile> threadProfileList(Long uid){
        return assemble(threadRepository.findAllActiveThreads(), uid);
    }

    public List<ThreadProfile> threadProfileList(Long user_uid,Long target_uid){
        return assemble(threadRepository.findAllUserThreads(target_uid), user_uid);
    }

    /**
     * Builds thread profiles for a feed using a fixed number of queries instead of
     * a handful per thread.
     *
     * Each thread previously triggered its own author lookup, follow lookups, like
     * lookup, and a full fetch of every comment row purely to call size() on it, so
     * cost grew linearly with the number of threads on screen. Everything the loop
     * needs is now fetched once up front and matched in memory.
     *
     * Visibility, ordering and comment counting are unchanged - including that the
     * count still includes soft-deleted comments, exactly as findByThread did.
     */
    private List<ThreadProfile> assemble(List<Thread_> threads, Long viewerUid){
        if(threads.isEmpty()){
            return List.of();
        }

        Map<Long, User_> authors = new HashMap<>();
        userRepository.findAllById(threads.stream().map(Thread_::uid).distinct().toList())
                .forEach(u -> authors.put(u.uid(), u));

        // Who the viewer follows, and who follows the viewer: two queries covering
        // every thread, rather than up to two per thread.
        Set<Long> viewerFollows = followService.getFollows(viewerUid).stream()
                .map(Follow_::receive_id).collect(Collectors.toSet());
        Set<Long> followsViewer = followService.getFollowers(viewerUid).stream()
                .map(Follow_::request_id).collect(Collectors.toSet());

        // Everybody blocked in either direction, fetched once for the whole feed
        // rather than asked per thread.
        Set<Long> invisible = blockService.invisibleTo(viewerUid);

        Set<Long> likedThreads = threadLikeRepository.findByUser(viewerUid).stream()
                .map(Threadlike_::tid).collect(Collectors.toSet());

        Map<Long, Long> commentCounts = new HashMap<>();
        commentRepository.countByThreadIds(threads.stream().map(Thread_::tid).toList())
                .forEach(c -> commentCounts.put(c.tid(), c.comment_count()));

        List<ThreadProfile> result = new ArrayList<>();
        for(Thread_ t : threads){
            User_ author = authors.get(t.uid());
            if(author == null){
                continue;
            }

            // A suspended or deleted account's posts go with it. findAllById is a
            // plain CRUD lookup and does not filter deleted_at the way every
            // query in UserRepository does, so without this a suspended account
            // keeps publishing.
            if(author.deleted_at() != null){
                continue;
            }

            // Checked before the privacy rule, because a block is the stronger
            // statement of the two: it does not matter whether a blocked person's
            // thread was public.
            if(invisible.contains(author.uid())){
                continue;
            }

            boolean mutualFollow = viewerFollows.contains(author.uid())
                    && followsViewer.contains(author.uid());
            if(!mutualFollow && t.is_private() && !t.uid().equals(viewerUid)){
                continue;
            }

            result.add(new ThreadProfile(t.tid(), UserView.from(author), t.title(), t.media(),
                    t.media_type(), t.category(), t.likes(),
                    likedThreads.contains(t.tid()),
                    commentCounts.getOrDefault(t.tid(), 0L),
                    t.created_at(), t.is_private()));
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
