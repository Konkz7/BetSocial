import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, Image, StyleSheet,SafeAreaView } from 'react-native';
import { ArrowLeft } from 'lucide-react-native';
import {DMCheck, fillReadMarkers, getConversations, getUser, makePrivateGroup} from "./API";
import { useFocusEffect } from '@react-navigation/native';
import { timeAgo } from './Constants';

  

const MessageScreen = ({ navigation , route } : any) => {

  const [conversations, setConversations] = useState<any[]>([]);

  const fetchConversations = async () => {
    try {
      const data = await getConversations();
      setConversations(data.map((entry:any) => ({
        ...entry,
        time: timeAgo(entry.time) 
      })));
    } catch (error) {
      console.error("Error fetching conversations:", error);
    }
  };

  async function goToDMScreen(uid:number, gid:number){

    
    const recipient : any = {};
    recipient["user"] = await getUser(uid);
    recipient["gid"] = gid;

    fillReadMarkers(recipient["gid"]);

    navigation.navigate("DMScreen_M",recipient);
    
  }

  useFocusEffect(
      useCallback(() => {

        fetchConversations();
        
        return () => {
          
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
        keyExtractor={(item) => item.index}
        renderItem={({ item }) => (   
          <View>
            <TouchableOpacity style={styles.conversationItem} onPress={() => goToDMScreen(item.uid, item.gid)}>
            <View style={styles.avatarContainer}>
              <Image source={{ uri: item.avatar }} style={styles.avatar} />
              {item.online && <View style={styles.onlineIndicator} />}
            </View>
            <View style={styles.messageInfo}>
              <View style={styles.messageHeader}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.time}>{item.time} ago</Text>
              </View>
              <Text style={[styles.lastMessage, item.unread && styles.unread]}>
                {item.lastMessage}
              </Text>
            </View>
            </TouchableOpacity>
          </View>
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
  },
});

export default MessageScreen;