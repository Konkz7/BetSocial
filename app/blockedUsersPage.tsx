import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  Image,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { ArrowLeft } from 'lucide-react-native';
import { getBlockedUsers, unblockUser } from './API';
import { getProfilePictureUrl } from './Constants';

/**
 * Who you have blocked, and the way back.
 *
 * A block has to be reversible somewhere, or the only way to undo one is to find
 * a person you have made invisible - which is the one thing blocking guarantees
 * you cannot do.
 *
 * There is no list of who has blocked you, and no endpoint that could build one.
 */
const BlockedUsersScreen = ({ navigation }: any) => {
  const [blocked, setBlocked] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    setBlocked(await getBlockedUsers());
    setLoading(false);
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  const confirmUnblock = (user: any) => {
    Alert.alert(
      `Unblock ${user.user_name}?`,
      'You will see each other again, and either of you can follow or message the other.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Unblock',
          onPress: async () => {
            setBusy(user.uid);
            const ok = await unblockUser(user.uid);
            setBusy(null);
            if (ok) { setBlocked(rows => rows.filter(row => row.uid !== user.uid)); }
          },
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity style={styles.back} onPress={() => navigation.goBack()}>
          <ArrowLeft size={28} color={'green'} />
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Blocked</Text>
      </View>

      <FlatList
        data={blocked}
        keyExtractor={item => item.uid.toString()}
        removeClippedSubviews={false}
        onRefresh={load}
        refreshing={loading}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {loading ? 'Loading…' : 'You have not blocked anybody.'}
          </Text>
        }
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Image
              source={getProfilePictureUrl(item.profile_picture)}
              style={styles.avatar}
            />
            <Text style={styles.name} numberOfLines={1}>{item.user_name}</Text>
            <TouchableOpacity
              style={styles.unblock}
              disabled={busy === item.uid}
              onPress={() => confirmUnblock(item)}
            >
              <Text style={styles.unblockText}>
                {busy === item.uid ? 'Working…' : 'Unblock'}
              </Text>
            </TouchableOpacity>
          </View>
        )}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fcfcf7' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 18,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  back: { flexDirection: 'row', alignItems: 'center' },
  backText: { fontSize: 16, color: 'green', marginLeft: 4 },
  headerTitle: { fontSize: 20, fontWeight: '700', color: '#111827', marginLeft: 16 },

  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    backgroundColor: 'white',
  },
  avatar: { width: 40, height: 40, borderRadius: 15 },
  name: { flex: 1, marginLeft: 12, fontSize: 16, color: '#111827' },
  unblock: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 8,
    backgroundColor: '#e6f4ea',
  },
  unblockText: { color: 'green', fontWeight: '600' },

  empty: { textAlign: 'center', marginTop: 60, color: '#6b7280' },
});

export default BlockedUsersScreen;
