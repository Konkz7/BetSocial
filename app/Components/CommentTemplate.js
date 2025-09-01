import React, { useState } from "react";
import { View, Text, Image, TouchableOpacity } from "react-native";
import { Heart, Reply, X } from "lucide-react-native"; // install lucide-react-native
import { timeAgo } from "../Constants";
import { registerCommentLike } from "../API";

const Comment = ({ comment, onReply, replyNum }) => {
  const [showReplies, setShowReplies] = useState(false);
  const [isLiked, setIsLiked] = useState(comment.liked);
  const [likes, setLikes] = useState(comment.likes);

  const isReplying = replyNum === comment.cid;

  const handleLike = (comment) => {
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
    <View style={{ marginVertical: 6, padding: 8, borderRadius:20, backgroundColor: isReplying ? "#f0f8ff" : null }}>
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
      <View style={{ flexDirection: "row", alignItems: "center", gap: 12 , marginTop: 10 }}>
        <TouchableOpacity 
          style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
          onPress={() => handleLike(comment)}
        >
          <Heart size={18} color={isLiked ? "red": "gray"} fill={isLiked ? "red" : "transparent"} />
          <Text style={{ fontSize: 12, color: "gray" }}>{likes}</Text>
          <Text style={{ fontSize: 12, color: "gray" }}>likes</Text>
        </TouchableOpacity>

        {!isReplying ? (
            <TouchableOpacity 
              style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
              onPress={() => onReply(comment)}
            >
              <Reply size={18} color="gray" />
              <Text style={{ fontSize: 12, color: "gray" }}>reply</Text>
            </TouchableOpacity>
          ) :
          (
            <TouchableOpacity 
              style={{ flexDirection: "row", alignItems: "center", gap: 4 }} 
              onPress={() => onReply(comment)}
            >
              <X size={18} color="red" />
              <Text style={{ fontSize: 12, color: "red" }}>Cancel</Text>
            </TouchableOpacity>
          )
        }

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
        <View 
          key={reply.cid} 
          style={{ marginTop: 6, marginLeft: 24, padding: 6, backgroundColor: isReplying ? "#f0f8ff" : null , borderRadius: 10 }}
        >
          <Comment comment={reply} onReply={onReply} replyNum={replyNum}/>
        </View>
      ))}
    </View>
  );
};
export function CommentList({ loadedComments ,replyNum ,setReplyNum}) {


  const handleReply = (comment) =>{

    if(replyNum === comment.cid){
      setReplyNum(-1);
    }else{
      setReplyNum(comment.cid);
      console.log("Replying to comment ID:", comment.cid);
    }

     
  }

  return (
    <View style={{alignSelf: "stretch" }}>
      {loadedComments.map(comment => (
        <Comment 
          key={comment.cid} 
          comment={comment} 
          onReply={handleReply} 
          replyNum={replyNum}
        />
      ))}
    </View>
  );
}