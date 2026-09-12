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
import { ArrowLeft, Send, Check, ImageUp, Users } from 'lucide-react-native';
import { useFocusEffect } from '@react-navigation/native';
import webSocketService from './Components/WebSocketService';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteMessage, fillReadMarkers, getChatMessages, getGroupMembers, updateLastTimestamp } from './API';
import { formatMessageTime, getProfilePictureUrl } from './Constants';
import { selectMedia } from './Components/FBStorageService';
import Video from 'react-native-video';
import { screenStore } from './GlobalFlags';

/**
 * A group conversation.
 *
 * Separate from dmPage rather than a mode inside it. The two look similar today,
 * but a direct conversation has one counterparty whose name, picture and online
 * state fill the header, and a group has none of that - it has a name, a member
 * count, and a sender to identify against every incoming message. Keeping them
 * apart means either can gain something the other has no use for.
 */

type Message = {
  id: number;
  text: string;
  sent: boolean;
  senderId: number;
  time: string;
  seen: boolean;
  type: number;
  deleted: boolean;
};

/**
 * One server row as the shape this screen renders.
 *
 * Outside the component and taking the viewer's id as an argument, so it closes
 * over nothing. Defined inside, it became a new dependency of the focus effect
 * every render - which is either a stale-closure bug or a reconnecting
 * websocket, depending on which way you silence the warning.
 */
const toMessage = (item: any, selfUid: number): Message => ({
  id: item.mid,
  text: item.description,
  sent: item.uid === selfUid,
  senderId: item.uid,
  time: formatMessageTime(item.created_at),
  seen: item.is_read,
  type: item.media_type,
  deleted: item.deleted_at !== null,
});

const GroupChatScreen = ({ navigation, route }: any) => {
  const { gid, name } = route.params;

  const [newMessage, setNewMessage] = useState('');
  // Newest first, rendered by an inverted list so index 0 sits at the bottom.
  const [messages, setMessages] = useState<Message[]>([]);

  // Where the older messages continue from, or null at the start of the chat.
  const [cursor, setCursor] = useState<any>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);

  const queryClient = useQueryClient();
  const self = queryClient.getQueryData(['user']) as any;

  const { data: members } = useQuery({
    queryKey: ['groupMembers' + gid],
    queryFn: () => getGroupMembers(gid),
  });

  const { refetch: refetchChat } = useQuery({
    queryKey: ['chat' + gid],
    queryFn: () => getChatMessages(gid),
    enabled: false,
  });

  // Who sent a message is a uid on the message and a name on the member list.
  // A DM never needed this: there were two people and you were one of them.
  const senderOf = (uid: number) =>
    members?.find((member: any) => member.uid === uid);

  /**
   * Loads the messages before the ones on screen. Fires from onEndReached,
   * which on an inverted list is the top. Guarded because FlatList fires it
   * more than once per scroll.
   */
  const loadOlder = async () => {
    if (!cursor || loadingOlder) { return; }

    setLoadingOlder(true);
    try {
      const page = await getChatMessages(gid, cursor);

      setMessages(prev => {
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

  // handleScroll and scrollViewRef are gone with the ScrollView: they decided
  // whether to auto-scroll to the end, and an inverted list already starts
  // there.

  const deleteText = (message: Message) => {
    if (message.sent === false || message.deleted === true) { return; }
    Alert.alert('Delete Message', 'Are you sure you want to delete this message?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'OK',
        onPress: async () => {
          setMessages(previous => previous.map(m =>
            m.id === message.id
              ? { ...m, text: 'This message was deleted', type: 0, deleted: true }
              : m));
          await deleteMessage(message.id, gid);
        },
      },
    ]);
  };

  const send = () => {
    webSocketService.sendMessage({
      gid,
      description: newMessage,
      media_type: 0,
    });
    setNewMessage('');
  };

  const sendMedia = async () => {
    const { mediaUri, media_type } = await selectMedia();
    webSocketService.sendMessage({
      gid,
      description: mediaUri,
      media_type: media_type === 'image' ? 1 : 2,
    });
    setNewMessage('');
  };

  useFocusEffect(
    useCallback(() => {
      screenStore.set('Chat');

      // Clears the backlog. Anything arriving while this screen is open is marked
      // read by the server, which knows the conversation is being watched.
      fillReadMarkers(gid);

      webSocketService.connect(self.uid, gid, (message: any) => {
        // Presence and delete events share the topic with messages.
        if (message.description === undefined && message.type !== undefined) {
          if (message.type === 'DELETE') {
            setMessages(previous => previous.map(m =>
              m.id === message.mid
                ? { ...m, text: ' This message was deleted', type: 0, deleted: true }
                : m));
          }
          return;
        }

        // A group has no single "is the other person reading" state, so presence
        // frames carry nothing this screen can show. A DM turns them into a
        // read receipt; here they are dropped.
        if (message.description === undefined && message.online !== undefined) {
          return;
        }

        // Prepended: the list is newest-first and rendered inverted, so index 0
        // is the bottom of the screen.
        setMessages(previous => [{
          id: message.mid,
          text: message.description,
          sent: message.uid === self.uid,
          senderId: message.uid,
          time: formatMessageTime(Date.now()),
          seen: message.is_read,
          type: message.media_type,
          deleted: false,
        }, ...previous]);
      });

      // The cursor goes with the messages: one left over from another
      // conversation would pull that history into this screen.
      setMessages([]);
      setCursor(null);

      refetchChat()
        .then(result => {
          const page = result.data;
          if (!page) {
            console.error('Error fetching chat messages:', result.error);
            return;
          }
          // Already newest-first from the server, which is what the inverted
          // list wants - nothing is reversed on the way in.
          setMessages(page.messages.map((m: any) => toMessage(m, self.uid)));
          setCursor(page.has_more
            ? { created_at: page.next_cursor_created_at, mid: page.next_cursor_mid }
            : null);
        })
        .catch(err => console.error('Refetch threw:', err));

      return () => {
        // Covers whatever arrived while the screen was open.
        updateLastTimestamp(gid);
        webSocketService.disconnect();
      };
    }, [self.uid, gid, refetchChat]),
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        {/* Tapping the header opens the member list, which is where adding,
            removing, renaming and leaving live. */}
        <Pressable
          style={styles.groupInfo}
          onPress={() => navigation.navigate('GroupMembersScreen_M', { gid, name })}
        >
          <View style={styles.groupIcon}>
            <Users size={20} color="#10B981" />
          </View>
          <View style={styles.groupText}>
            <Text style={styles.groupName} numberOfLines={1}>{name}</Text>
            <Text style={styles.groupMeta}>
              {members ? `${members.length} members` : ' '}
            </Text>
          </View>
        </Pressable>
      </View>

      {/* Inverted: the array is newest-first and index 0 renders at the bottom,
          so onEndReached fires at the top where older messages belong. Replaces
          a ScrollView that mounted every message in the conversation with no way
          to ask for more. */}
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
        renderItem={({ item: message, index }) => {
          const sender = message.sent ? null : senderOf(message.senderId);
          // Only label the first of a run from the same person - repeating the
          // name against every line of a burst is noise.
          //
          // index + 1, not index - 1: the list is newest-first and inverted, so
          // the message *above* this one on screen is the next one along the
          // array. Comparing backwards would put the name on the last line of
          // each burst instead of the first.
          const startsRun =
            !message.sent && messages[index + 1]?.senderId !== message.senderId;

          return (
            <View style={styles.messageRow}>
              {startsRun && (
                <View style={styles.senderLine}>
                  <Image
                    source={getProfilePictureUrl(sender?.profile_picture)}
                    style={styles.senderAvatar}
                  />
                  <Text style={styles.senderName}>
                    {sender?.user_name ?? 'Former member'}
                  </Text>
                </View>
              )}

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
                    <Text style={[
                      message.sent ? styles.textSent : styles.textReceived,
                      message.deleted ? styles.messageDeleted : {},
                    ]}>
                      {message.text}
                    </Text>
                  )}

                  {message.type === 1 && (
                    <View style={message.sent ? styles.imageSent : styles.imageReceived}>
                      <Image source={{ uri: message.text }} style={styles.media} />
                    </View>
                  )}

                  {message.type === 2 && (
                    <View style={message.sent ? styles.imageSent : styles.imageReceived}>
                      <Video
                        source={{ uri: message.text }}
                        style={styles.media}
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
                      {/* Green once every other member has opened the conversation
                          since this was sent - not just one of them. */}
                      <Check size={12} color={message.seen ? '#10B981' : '#9CA3AF'} />
                      <Check
                        size={12}
                        color={message.seen ? '#10B981' : '#9CA3AF'}
                        style={{ marginLeft: -3 }}
                      />
                    </View>
                  )}
                </View>
              </Pressable>
            </View>
          );
        }}
      />

      <View style={styles.inputRow}>
        <TouchableOpacity onPress={sendMedia} style={styles.mediaButton}>
          <ImageUp size={22} color="#10B981" />
        </TouchableOpacity>
        <TextInput
          style={styles.input}
          placeholder="Message"
          placeholderTextColor="#9CA3AF"
          value={newMessage}
          onChangeText={setNewMessage}
          multiline
        />
        <TouchableOpacity onPress={send} style={styles.sendButton}>
          <Send size={22} color="#10B981" />
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fcfcf7' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: { marginRight: 12 },
  groupInfo: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  groupIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#ECFDF5',
    alignItems: 'center',
    justifyContent: 'center',
  },
  groupText: { marginLeft: 12, flex: 1 },
  groupName: { fontSize: 16, fontWeight: '600', color: '#111827' },
  groupMeta: { fontSize: 13, color: '#6b7280' },

  messagesContainer: { flex: 1 },
  messagesContent: { padding: 12 },
  messageRow: { marginBottom: 6 },
  senderLine: { flexDirection: 'row', alignItems: 'center', marginBottom: 2, marginLeft: 4 },
  senderAvatar: { width: 20, height: 20, borderRadius: 10 },
  senderName: { marginLeft: 6, fontSize: 12, fontWeight: '600', color: '#6b7280' },

  messageWrapper: { maxWidth: '80%' },
  messageSent: { alignSelf: 'flex-end' },
  messageReceived: { alignSelf: 'flex-start' },
  messageBubble: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 14 },
  bubbleSent: { backgroundColor: '#10B981' },
  bubbleReceived: { backgroundColor: 'white' },
  textSent: { color: 'white', fontSize: 15 },
  textReceived: { color: '#111827', fontSize: 15 },
  messageDeleted: { fontStyle: 'italic', opacity: 0.7 },
  imageSent: { width: 200, height: 200, borderRadius: 10, overflow: 'hidden' },
  imageReceived: { width: 200, height: 200, borderRadius: 10, overflow: 'hidden' },
  media: { width: '100%', height: '100%' },
  messageMeta: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-end', marginTop: 2 },
  timeText: { fontSize: 10, color: '#9CA3AF' },
  checksContainer: { flexDirection: 'row', marginLeft: 4 },

  inputRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: 8,
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  mediaButton: { padding: 8 },
  sendButton: { padding: 8 },
  input: {
    flex: 1,
    maxHeight: 100,
    paddingHorizontal: 12,
    paddingVertical: 8,
    color: '#111827',
  },
});

export default GroupChatScreen;
