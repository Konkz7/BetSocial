import axios from "axios"; 
import { IP_STRING } from "./Constants";
import { Alert } from "react-native";

export const getProfile = async() =>{
    try {
        const profile = await axios.get(IP_STRING + "/req/profile");
        return (profile.data);
    } catch (error) {
      Alert.alert("Error!", "Profile couldnt be obtained.")
    }
  }

  //THREADS
// One page of the feed. Returns { threads, next_cursor_created_at,
// next_cursor_tid, has_more }.
//
// Pass the cursor from the previous response to get the next page; omit it for
// the first. The server decides whether there is more rather than leaving the
// client to infer it from a short page - with visibility rules in play a short
// page and the end of the feed used to look identical.
export const getThreads = async(cursor) =>{
  try {
      const query = cursor
        ? `?cursor_created_at=${cursor.created_at}&cursor_tid=${cursor.tid}`
        : "";
      const threads = await axios.get(IP_STRING + "/api/threads/active" + query);
      return threads.data;
  } catch (error) {
    Alert.alert("Error!", "Threads couldnt be obtained.")
    // An empty page rather than undefined: every caller reads .threads, and a
    // failed refresh should not look like the feed ending.
    return { threads: [], next_cursor_created_at: null, next_cursor_tid: null, has_more: false };
  }
}

export const toThreadProfile = async(tid) =>{
  try {
      const threadProfile = await axios.get(IP_STRING + "/api/threads/thread-profile/"+tid);
      return threadProfile.data;
  } catch (error) {
    Alert.alert("Error!", "Thread profile couldnt be obtained.")
  }
}


export const getUserThreads = async(uid) =>{
  try {
      const threads = await axios.get(IP_STRING + "/api/threads/user/"+uid);
      return threads.data;
  } catch (error) {
    Alert.alert("Error!", "Threads couldnt be obtained.")
  }
}

export const getThreadLikes = async() =>{
  try {
      const likes = await axios.get(IP_STRING + "/api/threads/thread-likes");
      return likes.data;
  } catch (error) {
    Alert.alert("Error!", "Likes couldnt be determined.");
  }
}

export const registerThreadLike = async(tid , liked) =>{
  try {
      const registerLike = await axios.put(IP_STRING + "/api/threads/register-like?tid="+tid+"&liked="+liked);
      return (registerLike.data);
  } catch (error) {
    Alert.alert("Error!", "Thread like couldnt be processed.")
  }
}

export const removeThread = async(tid) =>{
  try {
      const removedThread = await axios.put(IP_STRING + "/api/threads/remove/"+tid);
      return (removedThread.data);
  } catch (error) {
    Alert.alert("Error!", "Thread couldnt be removed.")
  }
}

// USERS
export const getUsers = async() =>{
    try {
        const users = await axios.get(IP_STRING + "/api/users/all");
        return users.data;
    } catch (error) {
      Alert.alert("Error!", "Users couldnt be obtained.")
    }
  }


  export const getUser = async(uid) =>{
    try {
        const user = await axios.get(IP_STRING + "/api/users/"+uid);
        return user.data;
    } catch (error) {
      Alert.alert("Error!", "User couldnt be found.")
    }
  }

  //GROUPS
  export const getGroupProfiles = async(uid) =>{
    try {
        const groupUsers = await axios.get(IP_STRING + "/api/groups/group-users");
        return groupUsers.data;
    } catch (error) {
      Alert.alert("Error!", "Groups couldnt be found.")
    }
  }

  export const DMCheck = async(uid) =>{
    try {
        const check = await axios.get(IP_STRING + "/api/groups/dm-check/"+uid);
        return check.data;
    } catch (error) {
      Alert.alert("Error!", "Check couldnt be done.")
    }
  }

  export const makePrivateGroup = async(other_uid) =>{
    try {
        const group = await axios.post(IP_STRING + "/api/groups/make/"+other_uid);
        return group.data;
    } catch (error) {
      Alert.alert("Error!", "Group couldnt be made.")
    }
  }

  // Creates a named group. The creator is taken from the session and becomes its
  // first administrator, so they are not listed in members.
  export const createGroup = async(group_name, members) =>{
    try {
        const group = await axios.post(IP_STRING + "/api/groups/create", { group_name, members });
        return group.data;
    } catch (error) {
      // The server explains why - too many members, a name already rejected, a
      // user that does not exist - and that is more use than a fixed sentence.
      Alert.alert("Group couldnt be made", error.response?.data?.message ?? error.message);
    }
  }

  // Members with the name, picture and administrator flag needed to draw them.
  export const getGroupMembers = async(gid) =>{
    try {
        const members = await axios.get(IP_STRING + "/api/groups/members/"+gid);
        return members.data;
    } catch (error) {
      Alert.alert("Error!", "Members couldnt be found.")
    }
  }

  // The server explains why it refused - not an administrator, group full, already
  // a member - and application.properties has include-message=always, so that
  // reaches us. Repeating it beats replacing it with a sentence of our own.
  const groupError = (error, fallback) =>
    Alert.alert(fallback, error.response?.data?.message ?? error.message);

  export const addGroupMember = async(gid, uid) =>{
    try {
        await axios.post(IP_STRING + "/api/groups/add-member/"+gid+"/"+uid);
        return true;
    } catch (error) {
      groupError(error, "Couldnt add them");
      return false;
    }
  }

  export const removeGroupMember = async(gid, uid) =>{
    try {
        await axios.delete(IP_STRING + "/api/groups/remove-member/"+gid+"/"+uid);
        return true;
    } catch (error) {
      groupError(error, "Couldnt remove them");
      return false;
    }
  }

  export const renameGroup = async(gid, group_name) =>{
    try {
        const group = await axios.put(IP_STRING + "/api/groups/rename/"+gid, { group_name });
        return group.data;
    } catch (error) {
      groupError(error, "Couldnt rename the group");
      return null;
    }
  }

  export const leaveGroup = async(gid) =>{
    try {
        await axios.delete(IP_STRING + "/api/groups/leave/"+gid);
        return true;
    } catch (error) {
      groupError(error, "Couldnt leave the group");
      return false;
    }
  }

  export const deleteGroup = async(gid) =>{
    try {
        await axios.delete(IP_STRING + "/api/groups/delete/"+gid);
        return true;
    } catch (error) {
      groupError(error, "Couldnt delete the group");
      return false;
    }
  }

  //PROFILE
  export const changeBio = async(text) =>{
    try {
        const changedBio = await axios.put(IP_STRING + "/api/users/change-bio?bio="+text);
        return (changedBio.data);
    } catch (error) {
      Alert.alert("Error!", "Bio couldnt be changed.")
    }
  }

  export const saveFBN = async(token) =>{
    try {
        const tokenSave = await axios.put(IP_STRING + "/api/users/save-FBNtoken?FBNtoken="+token);
        return (tokenSave.data);
    } catch (error) {
      Alert.alert("Error!", "FBN couldnt be saved.")
    }
  }

  export const changeUsername = async(name) =>{
    try {
        const newUserName = await axios.put(IP_STRING + "/api/users/change-user-name?newName="+ name);
        return (newUserName.data);
    } catch (error) {
      Alert.alert("Error!", "Name couldnt be changed.")
    }
  }

  export const changePfp = async(pfp) =>{
    try {
        const newPfp = await axios.put(IP_STRING + "/api/users/change-pfp?pfp="+ pfp);
        return (newPfp.data);
    } catch (error) {
      Alert.alert("Error!", "Profile picture couldnt be changed.")
    }
  }

//NOTIFICATIONS
export const getActiveNotifications = async() =>{
  try {
      const notifications = await axios.get(IP_STRING + "/api/notifications/active-notifications");
      return (notifications.data);
  } catch (error) {
    Alert.alert("Error!", "Notifications couldnt be fetched.")
  }
}

export const removeNotification = async(nid) =>{
  try {
      const deletedNotification = await axios.put(IP_STRING + "/api/notifications/delete/"+nid);
      return (deletedNotification.data);
  } catch (error) {
    Alert.alert("Error!", "Notification couldnt be deleted.")
  }
}

export const readNotifications = async() =>{
  try {
      const result = await axios.put(IP_STRING + "/api/notifications/update-read-markers");
      return (result.data);
  } catch (error) {
    Alert.alert("Error!", "Notifications couldnt be read.")
  }
}

//COMMENTS
export const createComment = async(comment) =>{
  try {
      const result = await axios.post(IP_STRING + "/api/comments/make",comment);
      return (result.data);
  } catch (error) {
    Alert.alert("Error!", "Comment couldnt be sent.")
  }
}

export const getComments = async(tid) =>{
  try {
      const comments = await axios.get(IP_STRING + "/api/comments/get-by-thread/"+tid);
      return comments.data;
  } catch (error) {
    Alert.alert("Error!", "Comments couldnt be found.");
  }
}

export const getCommentLikes = async(tid) =>{
  try {
      const likes = await axios.get(IP_STRING + "/api/comments/comment-likes/"+tid);
      return likes.data;
  } catch (error) {
    Alert.alert("Error!", "Likes couldnt be found.");
  }
}

export const registerCommentLike = async(cid,tid , liked) =>{
  try {
      const registerLike = await axios.put(IP_STRING + "/api/comments/register-like?tid="+tid+"&cid="+cid+"&liked="+liked);
      return (registerLike.data);
  } catch (error) {
    Alert.alert("Error!", "Comment like couldnt be processed.")
  }
}

export const deleteComment = async(cid) =>{
  try {
      const deletedComment = await axios.put(IP_STRING + "/api/comments/delete/"+cid);
      return (deletedComment.data);
  } catch (error) {
    Alert.alert("Error!", "Comment deletion couldnt be processed.")
  }
}


//FRIENDS

export const getFollow = async(ouid) =>{
  try {
      const follow = await axios.get(IP_STRING + "/api/follows/follow/"+ouid);
      return follow.data;
  } catch (error) {
    console.log("You dont follow this person");
  }
}

export const getOtherFollow = async(ouid) =>{
  try {
      const follow = await axios.get(IP_STRING + "/api/follows/other-follow/"+ouid);
      return follow.data;
  } catch (error) {
    console.log("You dont follow this person");
  }
}

export const follow = async(ouid) =>{
    try {
        const request = await axios.post(IP_STRING + "/api/follows/send/"+ouid);
        return (request.data);
    } catch (error) {
      Alert.alert("Error!", "Request couldnt be sent.")
    }
}

export const unfollow = async(ouid) =>{
  try {
      const unfollow = await axios.delete(IP_STRING + "/api/follows/unfollow/"+ouid);
      return (unfollow.data);
  } catch (error) {
    Alert.alert("Error!", "Request couldnt be sent.")
  }
}

export const getFollows = async() =>{
    try {
        const follows = await axios.get(IP_STRING + "/api/follows/follows");
        return (follows.data);
    } catch (error) {
      Alert.alert("Error!", "Follows couldnt be found.")
    }
}

export const getFollowers = async() =>{
    try {
        const followers = await axios.get(IP_STRING + "/api/follows/followers");
        return (followers.data);
    } catch (error) {
      Alert.alert("Error!", "Followers couldnt be found.")
    }
}

export const getFollowsByID = async(uid) =>{
    try {
        const follows = await axios.get(IP_STRING + "/api/follows/follows/" + uid);
        return (follows.data);
    } catch (error) {
      Alert.alert("Error!", "Follows couldnt be found.")
    }
}

export const getFollowersByID = async(uid) =>{
    try {
        const followers = await axios.get(IP_STRING + "/api/follows/followers/" + uid);
        return (followers.data);
    } catch (error) {
      Alert.alert("Error!", "Followers couldnt be found.")
    }
}


//MESSAGES
// One page of a conversation, newest first. Returns { messages,
// next_cursor_created_at, next_cursor_mid, has_more }.
//
// Newest first matches the order an inverted FlatList renders in - index 0 sits
// at the bottom - so nothing reverses the array on the way in. Pass the cursor
// from the previous response to fetch the messages *older* than it.
export const getChatMessages = async(gid, cursor) =>{
  try {
      const query = cursor
        ? `?cursor_created_at=${cursor.created_at}&cursor_mid=${cursor.mid}`
        : "";
      const messages = await axios.get(IP_STRING + "/api/messages/group/" + gid + query);
      return messages.data;
  } catch (error) {
    Alert.alert("Error!", "Chat messgaes couldnt be found.")
    // An empty page rather than undefined: a failed load must not look like the
    // start of the conversation, or the screen stops offering to load more.
    return { messages: [], next_cursor_created_at: null, next_cursor_mid: null, has_more: false };
  }
}

export const getConversations = async() =>{
  try {
      const conversations = await axios.get(IP_STRING + "/api/messages/conversations");
      return conversations.data;
  } catch (error) {
    Alert.alert("Error!", "Conversations couldnt be found.")
  }
}

export const fillReadMarkers = async(gid) =>{
  try {
      const fill = await axios.put(IP_STRING + "/api/messages/update-reads/"+gid);
      return fill.data;
  } catch (error) {
    Alert.alert("Error!", "Chat messages couldnt be read.")
  }
}

export const updateLastTimestamp = async(gid) =>{
  try {
      const stamp = await axios.put(IP_STRING + "/api/groups/update-timestamp/"+gid);
      return stamp.data;
  } catch (error) {
    Alert.alert("Error!", "Timestamp couldnt be updated.")
  }
}

// fillReadMarker is gone. It marked one message read, which is the per-message
// read state that groupuser_.last_read_timestamp replaced - the endpoint behind
// it no longer exists, and nothing had imported this since before it went.

export const deleteMessage = async(mid,gid) =>{
  try {
      const del = await axios.put(IP_STRING + "/api/messages/delete?mid="+mid+"&gid="+gid);
      return del.data;
  } catch (error) {
    Alert.alert("Error!", "Chat message couldnt be deleted.")
  }
}

//WALLET
// Every /circle/* call is gone. They reached CircleService, which was deleted
// with the crypto attempt long before this - get-secret, get-user-wallet,
// get-balance and create-card have all been 404ing ever since. getIpAddress went
// with them; it asked ipify where the phone was, for a card form that no longer
// exists.

// Balance and the movements that add up to it. Reading this also collects the
// daily top-up if one is due, which is why there is no button for that.
// Reading the wallet is also what collects the daily top-up, so this is a GET
// that changes something - see WalletController.
//
// silent is for callers that poll it rather than being opened deliberately: the
// home header refetches on every focus, and a modal per failure would be
// unusable. Those callers show a dash instead and the screen itself still says
// what went wrong when somebody opens it on purpose.
export const getWallet = async ({ silent = false } = {}) =>{
  try {
    const wallet = await axios.get(IP_STRING + "/api/wallet");
    return wallet.data;
  } catch (error) {
    if (!silent) {
      Alert.alert("Error!", "Your wallet couldnt be loaded.");
    }
    return null;
  }
}

//PREDICTIONS
// Places a stake. The coins leave immediately and cannot be taken back, so the
// server's refusal - too little in the wallet, outside the bet's limits, already
// predicted - is passed on rather than replaced.
export const makePrediction = async (bid, prediction, amount_bet) =>{
  try {
    await axios.post(IP_STRING + "/api/predictions/make", { bid, prediction, amount_bet });
    return true;
  } catch (error) {
    Alert.alert("Couldnt place that bet", error.response?.data?.message ?? error.message);
    return false;
  }
}

// The caller's own predictions, in one request rather than one per bet. A stake
// cannot be changed once placed, so this is what decides whether a bet is still
// open to this person.
export const getMyPredictions = async () =>{
  try {
    const mine = await axios.get(IP_STRING + "/api/predictions/mine");
    return mine.data;
  } catch (error) {
    console.log("Error!", "Couldnt load your predictions: " + error.message);
    return [];
  }
}


// Your own bets that have closed and are waiting on you to say what happened.
//
// silent for the same reason as getWallet: the home screen checks this on focus
// to decide whether to show a banner, and a modal on every failure would be
// unusable. The screen itself is opened deliberately and does report failures.
export const getBetsAwaitingMyDecision = async ({ silent = false } = {}) =>{
  try {
    const awaiting = await axios.get(IP_STRING + "/api/bets/awaiting-my-decision");
    return awaiting.data;
  } catch (error) {
    if (!silent) {
      Alert.alert("Error!", "Couldnt load the bets waiting on you.");
    }
    return [];
  }
}

// Declaring what happened. This does not pay anybody out - it hands the bet to
// an approver, who has no stake in it and signs the outcome off.
export const declareOutcome = async (bid, decision, reason) =>{
  try {
    await axios.post(IP_STRING + "/api/bets/decide", { bid, decision, reason });
    return true;
  } catch (error) {
    Alert.alert("Couldnt record that outcome", error.response?.data?.message ?? error.message);
    return false;
  }
}


//ADMIN
// The approval queue: bets whose owner has declared an outcome and which are
// waiting on somebody without a stake in them.
export const getPendingApprovals = async() =>{
  try {
      const pending = await axios.get(IP_STRING + "/superusers/bets/pending");
      return pending.data;
  } catch (error) {
    Alert.alert("Error!", "The approval queue couldnt be loaded.")
  }
}

// Approving pays the winners; rejecting returns every stake. Both are final.
export const approveBet = async(bid, decision, reason) =>{
  try {
      await axios.post(IP_STRING + "/superusers/approval", { bid, decision, reason });
      return true;
  } catch (error) {
    Alert.alert("Couldnt settle the bet", error.response?.data?.message ?? error.message);
    return false;
  }
}

//BLOCKING
// Apple's guideline 1.2 requires a way to block other users. A block is mutual:
// neither party sees the other afterwards.
export const blockUser = async (uid) =>{
  try {
    await axios.post(IP_STRING + "/api/blocks/" + uid);
    return true;
  } catch (error) {
    Alert.alert("Couldnt block them", error.response?.data?.message ?? error.message);
    return false;
  }
}

export const unblockUser = async (uid) =>{
  try {
    await axios.delete(IP_STRING + "/api/blocks/" + uid);
    return true;
  } catch (error) {
    Alert.alert("Couldnt unblock them", error.response?.data?.message ?? error.message);
    return false;
  }
}

// Only ever your own - there is no endpoint for anybody else's.
export const getBlockedUsers = async () =>{
  try {
    const blocked = await axios.get(IP_STRING + "/api/blocks");
    return blocked.data;
  } catch (error) {
    Alert.alert("Error!", "Your blocked list couldnt be loaded.");
    return [];
  }
}

//REPORTING
// Reporting reaches a moderator, unlike blocking, which is private to you.
// target_type is THREAD, COMMENT or USER.
export const reportContent = async (target_type, target_id, reason, detail) =>{
  try {
    await axios.post(IP_STRING + "/api/reports",
      { target_type, target_id, reason, detail });
    Alert.alert("Thanks", "A moderator will look at this.");
    return true;
  } catch (error) {
    // 409 is the normal "you already reported this" case rather than a fault,
    // so it gets its own wording.
    if (error.response?.status === 409) {
      Alert.alert("Already reported", "You have already reported this.");
    } else {
      Alert.alert("Couldnt report that", error.response?.data?.message ?? error.message);
    }
    return false;
  }
}

// Admin only. Each row carries the reported content, so the queue needs one call.
export const getOpenReports = async() =>{
  try {
    const open = await axios.get(IP_STRING + "/superusers/reports");
    return open.data;
  } catch (error) {
    Alert.alert("Error!", "The moderation queue couldnt be loaded.");
    return [];
  }
}

// action is REMOVED, SUSPENDED or DISMISSED.
export const decideReport = async(rid, action) =>{
  try {
    await axios.post(IP_STRING + "/superusers/reports/decide", { rid, action });
    return true;
  } catch (error) {
    Alert.alert("Couldnt action that report", error.response?.data?.message ?? error.message);
    return false;
  }
}
