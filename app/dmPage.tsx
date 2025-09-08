import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Image,
  StyleSheet,
  SafeAreaView,
} from 'react-native';
import { ArrowLeft, Send, Check,ImageUp } from 'lucide-react-native';
import { useFocusEffect } from '@react-navigation/native';
import webSocketService from './Components/WebSocketService';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getChatMessages,updateLastTimestamp } from './API';
import { formatMessageTime, timeAgo } from './Constants';
import { selectMedia } from './Components/FBStorageService';
import Video from 'react-native-video';


type Message = {
  id: number;
  text: string;
  sent: boolean; 
  time: string;
  seen: boolean;
  type: number; // 0 for text, 1 for image, 2 for video
  //TODO Seen will be fully implemented with the live feature , can be tested via postman.
};


const DMScreen = ({ navigation ,route }:any) => {
  const [newMessage, setNewMessage] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageIndex, setMessageIndex] = useState(0);
  const [isReading  , setIsReading] = useState(false);


  const recipient = route.params;
  const user = recipient.user;
  const chatGid = recipient.gid;

  const queryClient = useQueryClient();
  const self = queryClient.getQueryData(["user"]) as any;

  const getMIndex = () => {
    const index = messages.length + 1;
    setMessageIndex(index);
    return index;
  }

  const { data: chatData, refetch: refetchChat, isLoading: chatLoading } = useQuery({ 
      queryKey: ["chat"+chatGid], 
      queryFn: () =>getChatMessages(chatGid),
      enabled: false,
  });


 // console.log("Chat Data",chatData);

  useFocusEffect(
    useCallback(() => {
      // 1) Connect the socket

      webSocketService.connect(self.uid, chatGid, (message: any) => {
        
        if (message.description === undefined && message.online !== undefined) {
          setIsReading(message.online);

          // Optional: mark all my sent messages as seen when the recipient is reading
          if (message.online) {
            setMessages(prev =>
              prev.map(m =>
                m.sent ? { ...m, seen: true } : m
              )
            );
          }
          return; // stop here so we don't mis-treat as chat
        }

        const newMessageObj: Message = {
          id: getMIndex(),
          text: message.description,
          sent: message.uid == self.uid,
          time: formatMessageTime(Date.now()),
          seen: message.is_read,
          type: message.media_type,
        };
        setMessages(prevMessages => [...prevMessages, newMessageObj ]);
      });

      // 2) Clear any old messages
      setMessages([]);
  
      // 3) Fetch initial messages and *use the returned data*, not the stale chatData
      refetchChat()
        .then((result) => {
          const backendMessages = result.data;
          if (backendMessages) {
            const initialMessages: Message[] = backendMessages.map((item: any, idx: number) => ({
              id: idx + 1,
              text: item.description,
              sent: item.uid === self.uid,
              time: formatMessageTime(item.created_at),
              seen:  item.is_read,
              type: item.media_type,
            }));
  
            setMessages(initialMessages);
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
  

    const sendMessage = () => {

      
      const messageObj = {
        gid: chatGid,
        recipient_id: user.uid,
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
        recipient_id: user.uid,
        description: mediaUri,
        media_type: type,
      };
      webSocketService.sendMessage(messageObj);

      setNewMessage('');


    };

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <View style={styles.profileContainer}>
          <View style={styles.profileImageContainer}>
            <Image
              source={{
                uri: user.profile_picture, 
              }}
              style={styles.profileImage}
            /> 
            <View style={styles.onlineIndicator} />
          </View>
          <View style={styles.profileInfo}>
            <Text style={styles.profileName}>{user.user_name}</Text>
            <Text style={styles.profileStatus}>Online</Text>
          </View>
        </View>
      </View>

      {/* Messages */}
      <ScrollView style={styles.messagesContainer} contentContainerStyle={styles.messagesContent}>
        {messages.map((message) => (
          <View
            key={messages.indexOf(message)}
            style={[
              styles.messageWrapper,
              message.sent ? styles.messageSent : styles.messageReceived,
            ]}
          >
            <View
              style={[
                styles.messageBubble,
                message.sent ? styles.bubbleSent : styles.bubbleReceived,
              ]}
            >
             {message.type === 0 && (
              <Text style={message.sent ? styles.textSent : styles.textReceived}>
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
          </View>
        ))}
      </ScrollView>

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
