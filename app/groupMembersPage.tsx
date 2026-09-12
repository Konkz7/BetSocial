import React, { useMemo, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Image,
  StyleSheet,
  SafeAreaView,
  TextInput,
  Alert,
} from 'react-native';
import { ArrowLeft, Check, UserMinus, UserPlus, X } from 'lucide-react-native';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addGroupMember,
  deleteGroup,
  getGroupMembers,
  searchUsers,
  leaveGroup,
  removeGroupMember,
  renameGroup,
} from './API';
import { getProfilePictureUrl } from './Constants';

/**
 * Who is in a group, and the things you can do about it.
 *
 * The permission model is the server's; this screen only reflects it. An action
 * the caller is not allowed is not drawn, and the server refuses it anyway if it
 * somehow arrives - hiding a button is a courtesy, not a control.
 *
 *   add       any member
 *   remove    administrators
 *   rename    administrators
 *   leave     anyone
 *   delete    administrators
 */
const GroupMembersScreen = ({ navigation, route }: any) => {
  const { gid, name } = route.params;

  const queryClient = useQueryClient();
  const self = queryClient.getQueryData(['user']) as any;

  const [adding, setAdding] = useState(false);
  const [search, setSearch] = useState('');
  const [editingName, setEditingName] = useState(false);
  const [draftName, setDraftName] = useState(name);
  const [groupName, setGroupName] = useState(name);
  const [busy, setBusy] = useState(false);

  const { data: members, refetch: refetchMembers } = useQuery({
    queryKey: ['groupMembers' + gid],
    queryFn: () => getGroupMembers(gid),
  });

  // Searched on the server, and only while the add sheet is open. The name
  // filter that used to run below now runs in the database, capped - this screen
  // was fetching every account to show a handful.
  const { data: users } = useQuery({
    queryKey: ['userSearch', search],
    queryFn: () => searchUsers(search),
    enabled: adding,
    placeholderData: (previous: any) => previous,
  });

  const iAmAdmin = !!members?.find(
    (member: any) => member.uid === self.uid && member.administrator,
  );

  // Only people who are not already in it, so tapping one cannot fail for the one
  // reason the server would definitely give.
  const candidates = useMemo(() => {
    if (!users || !members) return [];
    // Still filtered here, because "not already a member" is about this group
    // rather than about the search - the server has no reason to know which
    // conversation the picker belongs to. The name matching moved to the query.
    const present = new Set(members.map((member: any) => member.uid));
    return users.filter((user: any) => !present.has(user.uid));
  }, [users, members]);

  // The chat screen reads this same key, so it picks up a changed membership
  // without being told about it separately. The conversation list is not a query
  // at all - it holds its rows in state and refetches whenever it regains focus -
  // so going back to it is already enough to refresh it.
  const refresh = () => refetchMembers();

  const add = async (uid: number) => {
    setBusy(true);
    const ok = await addGroupMember(gid, uid);
    setBusy(false);
    if (ok) { await refresh(); }
  };

  const remove = (member: any) => {
    Alert.alert(
      'Remove ' + member.user_name + '?',
      'They will lose access to this conversation.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: async () => {
            setBusy(true);
            const ok = await removeGroupMember(gid, member.uid);
            setBusy(false);
            if (ok) { await refresh(); }
          },
        },
      ],
    );
  };

  const saveName = async () => {
    if (!draftName.trim()) {
      Alert.alert('Name the group', 'A group needs a name so people can tell it apart.');
      return;
    }
    setBusy(true);
    const group = await renameGroup(gid, draftName.trim());
    setBusy(false);
    if (!group) { return; }

    setGroupName(group.group_name);
    setEditingName(false);

    // Back to the conversation, carrying the new name. The chat screen takes its
    // title from route params, and setParams here would only change this screen's
    // - so staying put would leave the header underneath showing the old name
    // until the conversation was next opened from scratch. Returning to it also
    // makes the rename visible, which is the confirmation.
    navigation.navigate('GroupChatScreen_M', { gid, name: group.group_name });
  };

  const leave = () => {
    Alert.alert(
      'Leave ' + groupName + '?',
      iAmAdmin
        ? 'You administer this group. Someone else will take that over.'
        : 'You will stop receiving its messages.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Leave',
          style: 'destructive',
          onPress: async () => {
            if (await leaveGroup(gid)) {
              // Back past the conversation as well - it is not yours any more.
              navigation.navigate('Messagescreen_M');
            }
          },
        },
      ],
    );
  };

  const destroy = () => {
    Alert.alert(
      'Delete ' + groupName + '?',
      'The conversation and every message in it go, for everyone. This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            if (await deleteGroup(gid)) {
              navigation.navigate('Messagescreen_M');
            }
          },
        },
      ],
    );
  };

  const header = (
    <View>
      <View style={styles.nameBlock}>
        {editingName ? (
          <View style={styles.nameEditRow}>
            <TextInput
              style={styles.nameInput}
              value={draftName}
              onChangeText={setDraftName}
              maxLength={100}
              autoFocus
            />
            <TouchableOpacity onPress={saveName} disabled={busy} style={styles.iconButton}>
              <Check size={22} color="#10B981" />
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => { setDraftName(groupName); setEditingName(false); }}
              style={styles.iconButton}
            >
              <X size={22} color="#9CA3AF" />
            </TouchableOpacity>
          </View>
        ) : (
          <TouchableOpacity
            disabled={!iAmAdmin}
            onPress={() => setEditingName(true)}
            style={styles.nameEditRow}
          >
            <Text style={styles.groupName}>{groupName}</Text>
            {iAmAdmin && <Text style={styles.renameHint}>Rename</Text>}
          </TouchableOpacity>
        )}
        <Text style={styles.count}>
          {members ? `${members.length} members` : ' '}
        </Text>
      </View>

      <TouchableOpacity style={styles.addRow} onPress={() => setAdding(true)}>
        <View style={styles.addIcon}>
          <UserPlus size={20} color="#10B981" />
        </View>
        <Text style={styles.addText}>Add people</Text>
      </TouchableOpacity>
    </View>
  );

  const footer = (
    <View style={styles.footer}>
      <TouchableOpacity onPress={leave} style={styles.dangerRow}>
        <Text style={styles.dangerText}>Leave group</Text>
      </TouchableOpacity>
      {iAmAdmin && (
        <TouchableOpacity onPress={destroy} style={styles.dangerRow}>
          <Text style={styles.dangerText}>Delete group</Text>
        </TouchableOpacity>
      )}
    </View>
  );

  // Two modes rather than a list inside a list: only one is on screen at a time,
  // which keeps a FlatList from being nested in another scrolling view.
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => (adding ? setAdding(false) : navigation.goBack())}
          style={styles.backButton}
        >
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{adding ? 'Add people' : 'Members'}</Text>
      </View>

      {adding ? (
        <>
          <TextInput
            style={styles.searchInput}
            placeholder="Search people"
            placeholderTextColor="#9CA3AF"
            value={search}
            onChangeText={setSearch}
          />
          <FlatList
            data={candidates}
            keyExtractor={item => item.uid.toString()}
            removeClippedSubviews={false}
            ListEmptyComponent={
              <Text style={styles.empty}>Nobody left to add</Text>
            }
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.row}
                disabled={busy}
                onPress={() => add(item.uid)}
              >
                <Image
                  source={getProfilePictureUrl(item?.profile_picture)}
                  style={styles.avatar}
                />
                <Text style={styles.memberName}>{item.user_name}</Text>
                <UserPlus size={20} color="#10B981" />
              </TouchableOpacity>
            )}
          />
        </>
      ) : (
        <FlatList
          data={members ?? []}
          keyExtractor={item => item.uid.toString()}
          removeClippedSubviews={false}
          ListHeaderComponent={header}
          ListFooterComponent={footer}
          renderItem={({ item }) => (
            <View style={styles.row}>
              <Image
                source={getProfilePictureUrl(item?.profile_picture)}
                style={styles.avatar}
              />
              <Text style={styles.memberName}>
                {item.uid === self.uid ? 'You' : item.user_name}
              </Text>
              {item.administrator && <Text style={styles.adminBadge}>Admin</Text>}
              {/* Removing yourself is what leaving is for, and the server says so
                  rather than doing it. */}
              {iAmAdmin && item.uid !== self.uid && (
                <TouchableOpacity
                  disabled={busy}
                  onPress={() => remove(item)}
                  style={styles.iconButton}
                >
                  <UserMinus size={20} color="#9E3B34" />
                </TouchableOpacity>
              )}
            </View>
          )}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fcfcf7' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: { marginRight: 12 },
  headerTitle: { fontSize: 18, fontWeight: '600', color: 'green' },

  nameBlock: { paddingHorizontal: 16, paddingVertical: 16, backgroundColor: 'white' },
  nameEditRow: { flexDirection: 'row', alignItems: 'center' },
  groupName: { flex: 1, fontSize: 20, fontWeight: '600', color: '#111827' },
  renameHint: { fontSize: 14, color: '#10B981', fontWeight: '500' },
  nameInput: {
    flex: 1,
    fontSize: 20,
    fontWeight: '600',
    color: '#111827',
    borderBottomWidth: 1,
    borderBottomColor: '#10B981',
    paddingVertical: 2,
  },
  count: { marginTop: 6, fontSize: 13, color: '#6b7280' },

  addRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    marginTop: 10,
  },
  addIcon: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: '#ECFDF5',
    alignItems: 'center',
    justifyContent: 'center',
  },
  addText: { marginLeft: 12, fontSize: 16, fontWeight: '500', color: '#10B981' },

  searchInput: {
    backgroundColor: 'white',
    margin: 16,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 10,
    color: '#111827',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  avatar: { width: 42, height: 42, borderRadius: 21 },
  memberName: { flex: 1, marginLeft: 12, fontSize: 16, fontWeight: '500' },
  adminBadge: {
    fontSize: 12,
    fontWeight: '600',
    color: '#10B981',
    backgroundColor: '#ECFDF5',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 10,
    overflow: 'hidden',
  },
  iconButton: { padding: 8, marginLeft: 4 },

  footer: { marginTop: 20, backgroundColor: 'white' },
  dangerRow: { paddingVertical: 14, paddingHorizontal: 16 },
  dangerText: { fontSize: 16, color: '#9E3B34', fontWeight: '500' },
  empty: { textAlign: 'center', marginTop: 40, color: '#6b7280' },
});

export default GroupMembersScreen;
