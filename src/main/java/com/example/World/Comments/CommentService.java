package com.example.World.Comments;

import com.example.World.Follows.FollowService;
import com.example.World.Follows.Follow_;
import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationService;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Threadlike_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ThreadRepository threadRepository;
    private final NotificationService notificationService;

    public CommentService(UserRepository userRepository, CommentRepository commentRepository, CommentLikeRepository commentLikeRepository, ThreadRepository threadRepository, FollowService followService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.threadRepository = threadRepository;
        this.notificationService = notificationService;
    }

    public Comment_ makeComment(CommentDTO comment,Long uid){

        Comment_ newComment = commentRepository.save(new Comment_(null, comment.tid(), uid, comment.parent_cid(),
                comment.description(),0L, new Date().getTime(),null,null));

        //thread owner
        Long toID = threadRepository.findById(comment.tid()).orElseThrow().uid();

        User_ threadOwner = userRepository.findById(toID).orElseThrow();



        NotificationDTO temp = new NotificationDTO(uid, "new_comment", comment.tid(), "thread");


        notificationService.registerNotification(threadOwner.fb_notification_token(), "Go check it out!" ,
                temp, toID);


        return newComment;
    }

    public List<CommentProfile> getComments(Long userID,Long tid) {
        List<Comment_> comments = commentRepository.findCommentsByTidAsc(tid);
        List<CommentProfile> rootComments = new ArrayList<>();
        Map<Long, CommentProfile> allComments = new HashMap<>();

// Pass 1: build all profiles
        for (Comment_ c : comments) {
            User_ user = userRepository.findById(c.uid()).orElseThrow();
            CommentProfile profile = new CommentProfile(
                    c.cid(), tid, c.uid(), new ArrayList<>(),
                    user.user_name(), user.profile_picture(),
                    c.parent_cid(), c.description(),
                    c.likes(),
                    commentLikeRepository.findByCommentAndUser(c.cid(), userID).isPresent(),
                    c.created_at(), c.deleted_at() != null
            );
            allComments.put(c.cid(), profile);
        }

// Pass 2: attach to parents
        for (CommentProfile cp : allComments.values()) {
            if (cp.parent_cid != null) {
                CommentProfile parent = allComments.get(cp.parent_cid);
                if (parent != null) {
                    parent.replies.add(cp);
                } else {
                    rootComments.add(cp); // orphaned → treat as root
                }
            } else {
                rootComments.add(cp);
            }
        }

        return rootComments;
    }

    public List<Commentlike_> getCommentLikes (Long uid , Long tid){
        return commentLikeRepository.findByThreadAndUser(tid,uid);
    }


    public void registerCommentLike(Long uid , Long cid, Long tid, boolean liked){
        Optional<Commentlike_> cl = commentLikeRepository.findByCommentAndUser(cid,uid);
        if(cl.isEmpty()){
            if(liked) {
                commentLikeRepository.save(new Commentlike_(null, tid,cid, uid));
                commentRepository.increment(cid);

                Long coID = commentRepository.findById(cid).orElseThrow().uid();

                if(coID.equals(uid)){return;}

                User_ commentOwner = userRepository.findById(coID).orElseThrow();



                NotificationDTO temp = new NotificationDTO(uid, "comment_like", tid, "thread");


                notificationService.registerNotification(commentOwner.fb_notification_token(), "Go check it out!"  ,
                        temp, coID);
            }
        }else{
            if(!liked){
                commentLikeRepository.delete(cl.get());
                commentRepository.decrement(cid);
            }
        }

    }

    @Transactional
    public void deleteComment(Long cid) {
        long now = System.currentTimeMillis();

        // soft delete this comment
        commentRepository.softDelete(cid, now);

        /*
        // fetch children and recursively delete them , if approach is slow do SQL approach
        List<Comment_> children = commentRepository.findCommentsByParentCID(cid);
        for (Comment_ child : children) {
            deleteComment(child.cid());
        }

         */
    }

}
