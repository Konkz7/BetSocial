import React from 'react';
import { View, Text, FlatList, TouchableOpacity, Image, StyleSheet,SafeAreaView } from 'react-native';
import { ArrowLeft } from 'lucide-react-native';

const conversations = [
  {
    id: 1,
    name: 'John Smith',
    lastMessage: "I'll take that bet! Let me know when...",
    time: '2m ago',
    unread: true,
    online: true,
    avatar: 'https://ui-avatars.com/api/?name=John+Smith&background=E5E7EB&color=4B5563',
  },
  {
    id: 2,
    name: 'Sarah Wilson',
    lastMessage: "The odds are looking good for tonight's game",
    time: '1h ago',
    unread: false,
    online: true,
    avatar: 'https://ui-avatars.com/api/?name=Sarah+Wilson&background=E5E7EB&color=4B5563',
  },
  {
    id: 3,
    name: 'Mike Johnson',
    lastMessage: 'Deal! 🤝',
    time: '3h ago',
    unread: false,
    online: false,
    avatar: 'https://ui-avatars.com/api/?name=Mike+Johnson&background=E5E7EB&color=4B5563',
  },
  {
    id: 4,
    name: 'Emma Davis',
    lastMessage: "What's your prediction for the match?",
    time: '1d ago',
    unread: false,
    online: false,
    avatar: 'https://ui-avatars.com/api/?name=Emma+Davis&background=E5E7EB&color=4B5563',
  },
];

const MessageScreen = ({ navigation , route } : any) => {

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Messages</Text>
      </View>
      <FlatList
        data={conversations}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <View style={styles.conversationItem}>
            <View style={styles.avatarContainer}>
              <Image source={{ uri: item.avatar }} style={styles.avatar} />
              {item.online && <View style={styles.onlineIndicator} />}
            </View>
            <View style={styles.messageInfo}>
              <View style={styles.messageHeader}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.time}>{item.time}</Text>
              </View>
              <Text style={[styles.lastMessage, item.unread && styles.unread]}>
                {item.lastMessage}
              </Text>
            </View>
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