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

  export const getUserThreads = async(uid) =>{
    try {
        const threads = await axios.get(IP_STRING + "/api/threads/user/"+uid);
        return threads.data;
    } catch (error) {
      Alert.alert("Error!", "Threads couldnt be obtained.")
    }
  }

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

  export const getGroupProfiles = async(uid) =>{
    try {
        const groupUsers = await axios.get(IP_STRING + "/api/groups/groups-users");
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

export const getFriends = async() =>{
  try {
      const friends = await axios.get(IP_STRING + "/api/friends/list");
      return friends.data;
  } catch (error) {
    Alert.alert("Error!", "Friends couldnt be found.");
  }
}

export const getFriendship = async(ouid) =>{
  try {
      const friendship = await axios.get(IP_STRING + "/api/friends/friendship/"+ouid);
      return friendship.data;
  } catch (error) {
    console.log("these guys arent friends");
  }
}


export const acceptFriendshipRequest = async(fid) =>{
  try {
      const accept = await axios.put(IP_STRING + "/api/friends/accept/"+fid);
      return accept.data;
  } catch (error) {
      Alert.alert("Error!", "Request couldnt be accepted.")
  }
}

export const rejectFriendshipRequest = async(fid) =>{
  try {
      const reject = await axios.put(IP_STRING + "/api/friends/reject/"+fid);
      return reject.data;
  } catch (error) {
      Alert.alert("Error!", "Request couldnt be rejected.")
  }
}


export const sendFriendRequest = async(ouid) =>{
    try {
        const request = await axios.post(IP_STRING + "/api/friends/send/"+ouid);
        return (request.data);
    } catch (error) {
      Alert.alert("Error!", "Request couldnt be sent.")
    }
}

export const unfriend = async(ouid) =>{
  try {
      const unfriend = await axios.delete(IP_STRING + "/api/friends/unfriend/"+ouid);
      return (unfriend.data);
  } catch (error) {
    Alert.alert("Error!", "Request couldnt be sent.")
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

export const getChatMessages = async(gid) =>{
  try {
      const messages = await axios.get(IP_STRING + "/api/messages/group/"+gid);
      return messages.data;
  } catch (error) {
    Alert.alert("Error!", "Chat messgaes couldnt be found.")
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

export const fillReadMarker = async(mid) =>{
  try {
      const fill = await axios.put(IP_STRING + "/api/messages/update-read/"+mid);
      return fill.data;
  } catch (error) {
    Alert.alert("Error!", "Chat message couldnt be read.")
  }
}


export const getCircleSecret = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-secret");
      return response.data;
    } catch (error) {
      Alert.alert("Error!", "Circle services are not secure/available: "+ error.message);
    }
  }

  export const getWallet = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-user-wallet");
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find user wallet: "+ error.message);
    }
  }

  export const getBalance = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-balance");
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find users' balance: "+ error.message);
    }
  }

  export const getIpAddress = async () =>{
    try {
      const response = await axios.get('https://api.ipify.org?format=json');
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find users' IP: "+ error.message);
    }
  }

  