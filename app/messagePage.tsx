import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, Image, StyleSheet,SafeAreaView } from 'react-native';
import { ArrowLeft, Smile } from 'lucide-react-native';
import {DMCheck, fillReadMarkers, getConversations, getUser, makePrivateGroup} from "./API";
import { useFocusEffect } from '@react-navigation/native';
import { timeAgo } from './Constants';
import { useQueryClient } from '@tanstack/react-query';
import { eventEmitter } from './Components/EventBus';
import { messageSeenStore, screenStore } from './GlobalFlags';

  

const MessageScreen = ({ navigation , route } : any) => {

  //fix seen issues

  const queryClient = useQueryClient();
  const self = queryClient.getQueryData(["user"]) as any;

  const [conversations, setConversations] = useState<any[]>([]);

  const fetchConversations = async () => {
    try {
      const data = await getConversations();
      setConversations(data);
      console.log(data);
    } catch (error) {
      console.error("Error fetching conversations:", error);
    }
  };

  const is_Unread_Conversations = async () => {   
    
    for (const conversation of conversations) {

      if (!conversation.lastMessage.is_read && conversation.lastMessage.recipient_id === self.uid) {
        messageSeenStore.set(false);
        break;
      }else{
        messageSeenStore.set(true)
      }
    }    

    console.log("Message seen status updated:", messageSeenStore.get())
  };

  async function goToDMScreen(uid:number, gid:number){

    
    const recipient : any = {};
    recipient["user"] = await getUser(uid);
    recipient["gid"] = gid;

    setConversations((prevConversations) =>
      prevConversations.map((conv) => {
        if (conv.gid === gid) {
          return { ...conv, lastMessage: { ...conv.lastMessage, is_read: true } };
        }
        return conv;
    }));  
    navigation.navigate("DMScreen_M",recipient);
    
  }

  useEffect(() => {
    if (conversations.length > 0) {
      is_Unread_Conversations();
    }
  }, [conversations]);

  useFocusEffect( 
      useCallback(() => {
        screenStore.set("Message");     
        // will cache conversations and automatically update when dm screen is focused
        fetchConversations();
        
    

        const sub =  eventEmitter.addListener('notificationReceived', (data : any) => {
          fetchConversations();
          console.log("Notification received via event bus" + data.title);    
        });

        
        return () => {
            sub.remove();
          
        };
      }, [])
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Messages</Text>
      </View>
      <FlatList
        data={conversations}
        keyExtractor={(item) => item.gid.toString()}
        removeClippedSubviews={false}
        ListEmptyComponent={<View style = {styles.notFound}>
                    <Smile size={50} color="gray" />
                    <Text>Your message list seems empty</Text>
                    </View>}
        renderItem={({ item }) => (   
          
            <TouchableOpacity style={styles.conversationItem} onPress={() => goToDMScreen(item.uid, item.gid)}> 
            <View style={styles.avatarContainer}>
              <Image source={{ uri: item.avatar}} style={styles.avatar} />
              {item.online && <View style={styles.onlineIndicator} />}
            </View>
            <View style={styles.messageInfo}>
              <View style={styles.messageHeader}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.time}>{timeAgo(item.lastMessage.created_at)} ago</Text>
              </View>
              <Text style={[styles.lastMessage, !item.lastMessage.is_read && item.lastMessage.recipient_id === self.uid && styles.unread]}>
                {item.lastMessage.description}
              </Text>
            </View>
            </TouchableOpacity>
          
        )}
      />
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
    paddingVertical: 20,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: {
    marginRight: 12,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: 'green',
  },
  conversationItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
    paddingHorizontal: 16,

  
  },
  avatarContainer: {
    position: 'relative',
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
  },
  onlineIndicator: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 10,
    height: 10,
    backgroundColor: 'green',
    borderRadius: 5,
    borderWidth: 2,
    borderColor: 'white',
  },
  messageInfo: {
    flex: 1,
    marginLeft: 12,
  },
  messageHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
  },
  time: {
    fontSize: 14,
    color: '#6b7280',
  },
  lastMessage: {
    fontSize: 14,
    color: '#6b7280',
  },
  unread: {
    color: '#111827',
    fontWeight: '600',
  },notFound:{
    marginTop: 50,
    justifyContent:"center",
    alignItems:"center",
    }
});

export default MessageScreen;