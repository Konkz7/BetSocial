package com.example.World.Comments;

import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public CommentService(UserRepository userRepository, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    public List<CommentProfile> getComments(Long tid) {
        List<Comment_> comments = commentRepository.findCommentsByTidAsc(tid);
        Map<Long, CommentProfile> allComments = new HashMap<>();
        List<CommentProfile> rootComments = new ArrayList<>();

        for (Comment_ c : comments) {
            User_ user = userRepository.findById(c.uid()).orElseThrow();

            // Build the profile
            CommentProfile profile = new CommentProfile(
                    c.cid(),
                    tid,
                    c.uid(),
                    new ArrayList<>(), // start with empty replies list
                    user.user_name(),
                    user.profile_picture(),
                    c.parent_cid(),
                    c.description(),
                    c.created_at()
            );

            // Put into global map
            allComments.put(c.cid(), profile);

            // If it has a parent, attach it to the parent's replies
            if (c.parent_cid() != null) {
                CommentProfile parent = allComments.get(c.parent_cid());
                if (parent != null) {
                    parent.replies.add(profile);
                } else {
                    // In case parent is not processed yet, ensure placeholder
                    allComments.putIfAbsent(c.parent_cid(),
                            new CommentProfile(c.parent_cid(), tid, null, new ArrayList<>(), null, null, null, null, null)
                    );
                    allComments.get(c.parent_cid()).replies.add(profile);
                }
            } else {
                // Top-level comment
                rootComments.add(profile);
            }
        }

        return rootComments;
    }

}
