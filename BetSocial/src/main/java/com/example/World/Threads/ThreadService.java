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

    /**
     * How many threads a feed request returns.
     *
     * Enough to fill a phone screen several times over, so scrolling stays ahead
     * of the network, and small enough that the first paint does not wait on
     * media for threads nobody has reached yet.
     */
    static final int FEED_PAGE_SIZE = 20;

    // There is no unpaginated feed any more. It returned every thread in the
    // database on every app launch, and keeping it alongside feedPage would mean
    // two answers to "what may this person see" - the thing this change exists to
    // stop.

    /**
     * One page of the feed.
     *
     * Visibility is decided in SQL rather than here - see
     * ThreadRepository.findFeedPage. Filtering after a LIMIT would hand back
     * fewer threads than the page asked for and, worse, could return an empty
     * page while more threads existed, which the client cannot tell apart from
     * the end of the feed.
     *
     * One extra row is fetched beyond the page size to answer "is there more"
     * without a second count query. It is dropped before the page is returned.
     */
    public FeedPage feedPage(Long viewerUid, Long cursorCreatedAt, Long cursorTid){
        // A cursor needs both halves or neither; half a cursor would silently
        // become "start from the beginning" and loop the client forever.
        if((cursorCreatedAt == null) != (cursorTid == null)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A cursor needs both cursor_created_at and cursor_tid");
        }

        List<Thread_> rows = threadRepository.findFeedPage(viewerUid, cursorCreatedAt,
                cursorTid, FEED_PAGE_SIZE + 1);

        boolean hasMore = rows.size() > FEED_PAGE_SIZE;
        List<Thread_> page = hasMore ? rows.subList(0, FEED_PAGE_SIZE) : rows;

        if(page.isEmpty()){
            return new FeedPage(List.of(), null, null, false);
        }

        Thread_ last = page.get(page.size() - 1);

        // assemble still joins the likes, comment counts and author views; it no
        // longer decides who may see what.
        return new FeedPage(assemble(page, viewerUid),
                hasMore ? last.created_at() : null,
                hasMore ? last.tid() : null,
                hasMore);
    }

    public List<ThreadProfile> threadProfileList(Long user_uid,Long target_uid){
        return assemble(threadRepository.findUserThreadsVisibleTo(user_uid, target_uid), user_uid);
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
     * Visibility is no longer decided here. It moved into the two feed queries,
     * because filtering in Java after a LIMIT hands back fewer threads than the
     * page asked for and can return an empty page while more threads exist. Both
     * callers now pass in rows the viewer is already entitled to see, and this
     * only joins on the likes, comment counts and author views.
     *
     * Ordering and comment counting are unchanged - including that the count
     * still includes soft-deleted comments, exactly as findByThread did.
     */
    private List<ThreadProfile> assemble(List<Thread_> threads, Long viewerUid){
        if(threads.isEmpty()){
            return List.of();
        }

        Map<Long, User_> authors = new HashMap<>();
        userRepository.findAllById(threads.stream().map(Thread_::uid).distinct().toList())
                .forEach(u -> authors.put(u.uid(), u));

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
