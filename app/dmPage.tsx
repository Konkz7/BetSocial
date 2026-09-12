import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Image,
  StyleSheet,
  SafeAreaView,
  Pressable,
  Alert,
} from 'react-native';
import { ArrowLeft, Send, Check,ImageUp } from 'lucide-react-native';
import { useFocusEffect } from '@react-navigation/native';
import webSocketService from './Components/WebSocketService';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteMessage, fillReadMarkers, getChatMessages,updateLastTimestamp } from './API';
import { formatMessageTime, getProfilePictureUrl, timeAgo } from './Constants';
import { selectMedia } from './Components/FBStorageService';
import Video from 'react-native-video';
import { screenStore } from './GlobalFlags';


type Message = {
  id: number;
  text: string;
  sent: boolean; 
  time: string;
  seen: boolean;
  type: number; // 0 for text, 1 for image, 2 for video
  deleted: boolean;
};


/**
 * One server row as the shape this screen renders.
 *
 * Outside the component and taking the viewer's id as an argument, so it closes
 * over nothing. Defined inside, it became a new dependency of the focus effect
 * on every render - which is either a stale closure or a websocket that
 * reconnects continuously, depending on which way you silence the warning.
 */
const toMessage = (item: any, selfUid: number): Message => ({
  id: item.mid,
  text: item.description,
  sent: item.uid === selfUid,
  time: formatMessageTime(item.created_at),
  seen: item.is_read,
  type: item.media_type,
  deleted: item.deleted_at !== null,
});

const DMScreen = ({ navigation ,route }:any) => {
  const [newMessage, setNewMessage] = useState('');
  // Newest first, so index 0 is the most recent. The inverted list renders that
  // at the bottom, which is where a chat starts.
  const [messages, setMessages] = useState<Message[]>([]);

  // Where the *older* messages continue from, or null at the start of the
  // conversation. Cleared alongside messages whenever the conversation changes.
  const [cursor, setCursor] = useState<any>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [isReading  , setIsReading] = useState(false);
  const [isOnline  , setIsOnline] = useState(false);




  const recipient = route.params;
  const user = recipient.user;
  const chatGid = recipient.gid;

  const queryClient = useQueryClient();
  const self = queryClient.getQueryData(["user"]) as any;

 

  const { data: chatData, refetch: refetchChat, isLoading: chatLoading } = useQuery({ 
      queryKey: ["chat"+chatGid], 
      queryFn: () =>getChatMessages(chatGid),
      enabled: false,
  });

  /**
   * Loads the messages before the ones on screen.
   *
   * Fires from onEndReached, which on an inverted list is the top - where older
   * messages belong. Guarded on loadingOlder because FlatList fires it more than
   * once per scroll, and two requests with the same cursor would append the same
   * page twice.
   */
  const loadOlder = async () => {
    if (!cursor || loadingOlder) return;

    setLoadingOlder(true);
    try {
      const page = await getChatMessages(chatGid, cursor);

      setMessages(prev => {
        // Appended, because the array is newest-first and these are older than
        // everything already in it. A websocket message arriving mid-fetch can
        // also land here, so ids are de-duplicated - a repeated key in a
        // FlatList is a crash, not a cosmetic fault.
        const seen = new Set(prev.map(m => m.id));
        return [...prev, ...page.messages.map((m: any) => toMessage(m, self.uid)).filter((m: Message) => !seen.has(m.id))];
      });

      setCursor(page.has_more
        ? { created_at: page.next_cursor_created_at, mid: page.next_cursor_mid }
        : null);
    } finally {
      setLoadingOlder(false);
    }
  };

  // handleScroll and scrollViewRef are gone with the ScrollView. They existed to
  // decide whether to auto-scroll to the end on a new message; an inverted list
  // already starts at the newest, so there is nothing to scroll to.

 
  const deleteText = (message : any) => {

    if(message.sent === false || message.deleted === true){return;}
    Alert.alert("Delete Message", "Are you sure you want to delete this message?", [
        {
            text: "Cancel",
            style: "cancel",
        },
        {
            text: "OK",
            onPress: async () => {
                setMessages(prevMessages => prevMessages.map(m => m.id === message.id ? { ...m, text: "This message was deleted", type:0 , deleted: true} : m));
                await deleteMessage(message.id,chatGid);
            },
        },
    ]);

  };

  const sendMessage = () => {

    const messageObj = {
      gid: chatGid,
      description: newMessage,
      media_type: 0, 
    };
    webSocketService.sendMessage(messageObj);
    console.log("IS READING:", isReading);

    setNewMessage('');

  };

  const sendMedia = async () => {

    const { mediaUri, media_type } = await selectMedia();
    const type = media_type === 'image' ? 1 : 2;

    const messageObj = {
      gid: chatGid,
      description: mediaUri,
      media_type: type,
    };
    webSocketService.sendMessage(messageObj);

    setNewMessage('');
  };

  useFocusEffect(
    useCallback(() => {
      // 1) Connect the socket
      screenStore.set("Chat"); 

      fillReadMarkers(recipient["gid"]);

      setIsOnline(user?.status === "offline" ? false : true);
      

      webSocketService.connect(self.uid, chatGid, (message: any) => {
        
        if (message.description === undefined && message.type !== undefined) {
          if(message.type === "DELETE"){
            setMessages(prev => prev.map(m => m.id === message.mid ? { ...m, text: " This message was deleted", type:0 , deleted: true} : m));
          }  
          return; // stop here so we don't mis-treat as chat
        }

        if (message.description === undefined && message.online !== undefined) {

          if(message.chatOnline){
            setIsReading(message.online);

            // Optional: mark all my sent messages as seen when the recipient is reading
            if (message.online) {
              setMessages(prev =>
                prev.map(m =>
                  m.sent ? { ...m, seen: true } : m
                )
              );
            }
          }else{
            setIsOnline(message.online);
          }
          
          return; // stop here so we don't mis-treat as chat
        }

        const newMessageObj: Message = {
          id: message.mid,
          text: message.description,
          sent: message.uid == self.uid,
          time: formatMessageTime(Date.now()),
          seen: message.is_read,
          type: message.media_type,
          deleted: false,
        };
        // Prepended, not appended: the list is newest-first now, so a new
        // message belongs at index 0. Appending would put it at the far end of
        // the history, which an inverted list renders at the very top.
        setMessages(prevMessages => [newMessageObj, ...prevMessages ]);
      });

      // 2) Clear any old messages, and the cursor with them - a cursor left over
      // from another conversation would load that one's history into this one.
      setMessages([]);
      setCursor(null);

      // 3) Fetch the newest page and *use the returned data*, not the stale chatData
      refetchChat()
        .then((result) => {
          const page = result.data;
          if (page) {
            // Already newest-first from the server, which is the order the
            // inverted list wants.
            setMessages(page.messages.map((m: any) => toMessage(m, self.uid)));
            setCursor(page.has_more
              ? { created_at: page.next_cursor_created_at, mid: page.next_cursor_mid }
              : null);
          } else {
            console.error("Error fetching chat messages:", result.error);
          }
        })
        .catch(err => console.error("Refetch threw:", err));
  
      // 4) Cleanup on unmount
      return () => {
        updateLastTimestamp(chatGid);

        webSocketService.disconnect();
      };
    }, [self.uid, chatGid, refetchChat])
  );

   

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <Pressable onPress = { () => navigation.navigate("Search", {screen: "Profile_S",params: user})} style={styles.profileContainer}>
          <View style={styles.profileImageContainer}>
            <Image
                source={getProfilePictureUrl(user?.profile_picture)}
                style={styles.profileImage}
            /> 
            { !isOnline ? <View style={[styles.onlineIndicator, {backgroundColor : "red"}]}></View> 
            : <View style={styles.onlineIndicator} />} 
          </View>
          <View style={styles.profileInfo}>
            <Text style={styles.profileName}>{user?.user_name}</Text>      
            { !isOnline ? <Text style={[styles.profileStatus, {color : "red"}]}>Offline</Text> 
            : <Text style={styles.profileStatus}>Online</Text>}
          </View>
        </Pressable>
      </View>

      {/* Messages
          Inverted, which is what makes this both paged and virtualised. The
          array is newest-first and `inverted` flips it visually, so index 0
          renders at the bottom where the newest message belongs - and
          onEndReached then fires at the *top*, which is exactly where "load
          older" goes.
          A ScrollView stood here and mounted every message in the conversation
          at once, with no hook to load more. Scrolling to the bottom on a new
          message is also gone: with an inverted list the bottom is where the
          list already starts. */}
      <FlatList
        style={styles.messagesContainer}
        contentContainerStyle={styles.messagesContent}
        data={messages}
        inverted
        keyExtractor={(item) => String(item.id)}
        removeClippedSubviews={false}
        showsVerticalScrollIndicator={false}
        overScrollMode="never"
        keyboardShouldPersistTaps="handled"
        onEndReached={loadOlder}
        onEndReachedThreshold={0.4}
        ListFooterComponent={loadingOlder
          ? <ActivityIndicator size="small" color="#10B981" style={{ marginVertical: 12 }} />
          : null}
        renderItem={({ item: message }) => (
          <Pressable
            style={[
              styles.messageWrapper,
              message.sent ? styles.messageSent : styles.messageReceived,
            ]}
            onLongPress={() => deleteText(message)}
          >
            <View
              style={[
                styles.messageBubble,
                message.sent ? styles.bubbleSent : styles.bubbleReceived,
              ]}
            >
             {message.type === 0 && (
              <Text style={[  message.sent ? styles.textSent : styles.textReceived , message.deleted ? styles.messageDeleted : {}]}>
                {message.text}
              </Text>
             )} 

             {message.type === 1 && (
              <View style={message.sent ? styles.imageSent : styles.imageReceived}>
                <Image
                  source={{ uri: message.text }}
                  style={{ width: '100%', height: '100%' }}
                />
              </View>
            )}

            {message.type === 2 && (
              <View style={message.sent ? styles.imageSent : styles.imageReceived}>
                <Video
                  source={{ uri: message.text }}
                  style={{ width: '100%', height: '100%' }}
                  controls={true}
                  paused={true}
                  resizeMode="cover"
                />
              </View>
            )}

            </View>
            <View style={styles.messageMeta}>
              <Text style={styles.timeText}>{message.time}</Text>
              {message.sent && (
                <View style={styles.checksContainer}>
                  <Check
                    size={12}
                    color={message.seen ? "#10B981" : "#9CA3AF"}
                  />
                  <Check
                    size={12}
                    color={message.seen ? "#10B981" : "#9CA3AF"}
                    style={{ marginLeft: -3 }}
                  />
                </View>
              )}
            </View>
          </Pressable>
        )}
      />

      {/* Input Area */}
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder="Type a message..."
          placeholderTextColor="#6B7280"
          value={newMessage}
          onChangeText={setNewMessage}
        />
        <TouchableOpacity style={styles.sendButton} onPress={() => sendMessage()}>
          <Send size={20} color="#fff" />
        </TouchableOpacity>

        <TouchableOpacity style={styles.sendButton} onPress={()=>sendMedia()}>
          <ImageUp size={20} color="#fff" />
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fcfcf7',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderBottomColor: '#E5E7EB',
    borderBottomWidth: 1,
    backgroundColor: '#fff',
  },
  backButton: {
    padding: 4,
  },
  profileContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 12,
  },
  profileImageContainer: {
    position: 'relative',
  },
  profileImage: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  onlineIndicator: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 8,
    height: 8,
    backgroundColor: '#10B981',
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#fff',
  },
  profileInfo: {
    marginLeft: 8,
  },
  profileName: {
    fontWeight: '600',
    fontSize: 16,
    color: '#1F2937',
  },
  profileStatus: {
    fontSize: 12,
    color: '#10B981',
  },
  messagesContainer: {
    flex: 1,
    paddingHorizontal: 12,
  },
  messagesContent: {
    paddingVertical: 20,
  },
  messageWrapper: {
    marginBottom: 16,
    maxWidth: '75%',
  },
  messageSent: {
    alignSelf: 'flex-end',
  },
  messageDeleted:{
    fontStyle: 'italic',
    color: 'rgba(2, 19, 114, 1)',
  },
  messageReceived: {
    alignSelf: 'flex-start',
  },
  messageBubble: {
    borderRadius: 16,
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  bubbleSent: {
    backgroundColor: '#10B981',
    borderBottomRightRadius: 0,
  },
  bubbleReceived: {
    backgroundColor: '#eee',
    borderBottomLeftRadius: 0,
  },
  textSent: {
    color: '#fff',
  },
  textReceived: {
    color: '#1F2937',
  },
  messageMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },
  timeText: {
    fontSize: 12,
    color: '#6B7280',
  },
  checksContainer: {
    flexDirection: 'row',
    marginLeft: 6,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderTopColor: '#E5E7EB',
    borderTopWidth: 1,
    backgroundColor: '#fff',
  },
  input: {
    flex: 1,
    backgroundColor: '#F3F4F6',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    fontSize: 16,
    color: '#1F2937',
  },
  sendButton: {
    backgroundColor: '#10B981',
    width: 40,
    height: 40,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 8,
  },
  imageSent: {
    width: 200,
    height: 200,
    borderRadius:20,
    overflow: 'hidden', // so rounded corners work
    backgroundColor: '#10B981', // match bubble color if needed
  },
    imageReceived: {
    width: 200,
    height: 200,
    borderRadius:10,
    overflow: 'hidden',
    backgroundColor: '#eee',
  },
});

export default DMScreen;
