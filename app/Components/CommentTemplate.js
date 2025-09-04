import React, { useState } from "react";
import { View, Text, Image, TouchableOpacity, Pressable, Alert } from "react-native";
import { Heart, Reply, X } from "lucide-react-native"; // install lucide-react-native
import { timeAgo } from "../Constants";
import { registerCommentLike,deleteComment } from "../API";

const Comment = ({ comment, onReply, replyNum , handleDelete}) => {
  const [showReplies, setShowReplies] = useState(false);
  const [isLiked, setIsLiked] = useState(comment.liked);
  const [likes, setLikes] = useState(comment.likes);

  const isReplying = replyNum === comment.cid;
  

  const handleLike = () => {
    console.log("Liked comment:", comment.cid);
  
    if(isLiked){
      setLikes(likes - 1);
    }else{
      setLikes(likes + 1);
    }
    setIsLiked(!isLiked);
    registerCommentLike(comment.cid,comment.tid ,!isLiked);
  };

  

  //console.log(comment.cid);

  return (
    <Pressable style={{ marginVertical: 6, padding: 8, borderRadius:20, backgroundColor: isReplying ? "#f0f8ff" : null }} onLongPress = {() => handleDelete(comment)}>
      {/* Header: Avatar + Username */}
      <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 4 }}>
        <Image source={{ uri: comment.profile_picture }} style={{ width: 36, height: 36, borderRadius: 18, marginRight: 8 }} />
        <View>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
              <Text style={{ fontWeight: "600", fontSize: 14 }}>{comment.user_name}</Text>
              <Text style={{ fontWeight: "600", fontSize: 11 , color: "gray", marginLeft: 6 , alignSelf: "flex-start" }}>{timeAgo(comment.created_at)}</Text>
            </View>
            {/* Body: Comment text */}
            <Text style={{ fontSize: 14, marginTop: 6 }}>{comment.description}</Text>
        </View>
      </View>

     

      {/* Actions: Like + Reply */}
      
        <View style={{ flexDirection: "row", alignItems: "center", gap: 12, marginTop: 10 }}>
          {!comment.deleted && (
            <>
            {/* Like button */}
            <TouchableOpacity 
              style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
              onPress={() => handleLike()}
            >
              <Heart 
                size={18} 
                color={isLiked ? "red" : "gray"} 
                fill={isLiked ? "red" : "transparent"} 
              />
              <Text style={{ fontSize: 12, color: "gray" }}>{likes}</Text>
              <Text style={{ fontSize: 12, color: "gray" }}>likes</Text>
            </TouchableOpacity>

            {/* Reply / Cancel toggle */}
            {!isReplying ? (
              <TouchableOpacity 
                style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
                onPress={() => onReply(comment)}
              >
                <Reply size={18} color="gray" />
                <Text style={{ fontSize: 12, color: "gray" }}>reply</Text>
              </TouchableOpacity>
            ) : (
              <TouchableOpacity 
                style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
                onPress={() => onReply(comment)}
              >
                <X size={18} color="red" />
                <Text style={{ fontSize: 12, color: "red" }}>Cancel</Text>
              </TouchableOpacity>
            )}
          </>
        )}

        {comment.replies?.length > 0 && (
          <TouchableOpacity onPress={() => setShowReplies(!showReplies)}>
            <Text style={{ fontSize: 12, color: "blue" }}>
              {showReplies ? "Hide Replies" : `View Replies (${comment.replies.length})`}
            </Text>
          </TouchableOpacity>
        )}
      </View>

      
      

      {/* Nested replies */}
      {showReplies && comment.replies?.map(reply => (
        <Pressable 
          key={reply.cid} 
          style={{ marginTop: 6, marginLeft: 24, padding: 6, backgroundColor: isReplying ? "#f0f8ff" : null , borderRadius: 10 }} onLongPress = {() => handleDelete(reply)}
        >
          <Comment comment={reply} onReply={onReply} replyNum={replyNum} handleDelete={handleDelete}/>
        </Pressable>
      ))}
    </Pressable>
  );
};
export function CommentList({ loadedComments, setLoadedComments ,replyNum ,setReplyNum , uid ,}) {


  const removeCommentRecursive = (comments, cidToRemove) => {
  return comments
    .map(c => ({
      ...c,
      deleted: c.cid === cidToRemove ? true : c.deleted,
      description: c.cid === cidToRemove ? "[deleted]" : c.description,
      replies: c.replies ? removeCommentRecursive(c.replies, cidToRemove) : []
    }));
  };
  

  const handleReply = (comment) =>{

    if(replyNum === comment.cid){
      setReplyNum(-1);
    }else{
      setReplyNum(comment.cid);
      console.log("Replying to comment ID:", comment.cid);
    }

     
  }

  const deleteCommentAction = (comment) => {
    const ownComment = uid === comment.uid;
    if(!ownComment || comment.deleted) return;

    // Show a confirmation dialog before deleting
    Alert.alert(
      "Delete Comment",
      "Are you sure you want to delete this comment?",
      [
        { text: "Cancel", style: "cancel" },
        { text: "Delete", 
          onPress: async () => {
            try {
              await deleteComment(comment.cid);
              
              // Recursively remove from state
              setLoadedComments(prev =>
                removeCommentRecursive(prev, comment.cid)
              );
              
              
            } catch (err) {
              console.error("Failed to delete comment:", err);
            }
          } 
        }
      ]
    );

  }

  return (
    <View style={{alignSelf: "stretch" }}>
      {loadedComments.map(comment => (
        <Comment 
          key={comment.cid} 
          comment={comment} 
          onReply={handleReply} 
          replyNum={replyNum}
          handleDelete={deleteCommentAction}
        />
      ))}
    </View>
  );
}