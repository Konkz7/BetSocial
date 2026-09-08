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
import { ArrowLeft, Check } from 'lucide-react-native';
import { useQuery } from '@tanstack/react-query';
import { createGroup, getUsers } from './API';
import { getProfilePictureUrl } from './Constants';

// Mirrors GroupService.MAX_GROUP_MEMBERS. The server rejects anything larger; this
// only spares the round trip and says so before the button is pressed.
const MAX_MEMBERS = 50;

const CreateGroupScreen = ({ navigation }: any) => {
  const [groupName, setGroupName] = useState('');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<any[]>([]);
  const [creating, setCreating] = useState(false);

  const { data: users, isLoading } = useQuery({ queryKey: ['Users'], queryFn: getUsers });

  const visible = useMemo(() => {
    if (!users) return [];
    if (!search) return users;
    return users.filter((user: any) =>
      user.user_name.toLowerCase().includes(search.toLowerCase()),
    );
  }, [users, search]);

  const isSelected = (uid: number) => selected.some(user => user.uid === uid);

  const toggle = (user: any) => {
    if (isSelected(user.uid)) {
      setSelected(previous => previous.filter(chosen => chosen.uid !== user.uid));
      return;
    }

    // The creator counts towards the cap as well, so the list can hold one fewer.
    if (selected.length + 1 >= MAX_MEMBERS) {
      Alert.alert('That is enough people', `A group can hold ${MAX_MEMBERS} members.`);
      return;
    }

    setSelected(previous => [...previous, user]);
  };

  const submit = async () => {
    if (!groupName.trim()) {
      Alert.alert('Name the group', 'A group needs a name so people can tell it apart.');
      return;
    }
    if (selected.length === 0) {
      Alert.alert('Choose somebody', 'A group with nobody else in it is just a note to self.');
      return;
    }

    // Guards the double tap: creating twice would leave two identical groups, and
    // unlike a direct conversation there is nothing to match them up by.
    setCreating(true);
    const group = await createGroup(groupName.trim(), selected.map(user => user.uid));
    setCreating(false);

    if (!group) return;

    // Replace rather than push: coming back from the group should return to the
    // conversation list, not to the form that made it.
    navigation.replace('GroupChatScreen_M', {
      gid: group.gid,
      name: group.group_name,
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New group</Text>
        <TouchableOpacity onPress={submit} disabled={creating}>
          <Text style={[styles.create, creating && styles.createDisabled]}>
            {creating ? 'Creating…' : 'Create'}
          </Text>
        </TouchableOpacity>
      </View>

      <TextInput
        style={styles.nameInput}
        placeholder="Group name"
        placeholderTextColor="#9CA3AF"
        value={groupName}
        onChangeText={setGroupName}
        maxLength={100}
      />

      <Text style={styles.count}>
        {selected.length === 0
          ? 'Nobody chosen yet'
          : `${selected.length} chosen`}
      </Text>

      <TextInput
        style={styles.searchInput}
        placeholder="Search people"
        placeholderTextColor="#9CA3AF"
        value={search}
        onChangeText={setSearch}
      />

      <FlatList
        data={visible}
        keyExtractor={item => item.uid.toString()}
        // Every other list in the app sets this, and for the same reason: with
        // clipping on, Android detaches rows as they scroll out and then fails to
        // find them again when the screen unmounts - "Cannot remove child from
        // parent, index out of range". Leaving it at the platform default was an
        // omission here, not a decision.
        removeClippedSubviews={false}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {isLoading ? 'Loading people…' : 'Nobody matches that'}
          </Text>
        }
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.row} onPress={() => toggle(item)}>
            <Image
              source={getProfilePictureUrl(item?.profile_picture)}
              style={styles.avatar}
            />
            <Text style={styles.name}>{item.user_name}</Text>
            {/* Always rendered and coloured in or out, rather than added and
                removed. Selecting somebody would otherwise change how many
                children the row has, which is the other half of what makes the
                native view tree and React's disagree about what is where. */}
            <Check size={20} color={isSelected(item.uid) ? '#10B981' : 'transparent'} />
          </TouchableOpacity>
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
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: { marginRight: 12 },
  headerTitle: { fontSize: 18, fontWeight: '600', color: 'green', flex: 1 },
  create: { fontSize: 16, fontWeight: '600', color: '#10B981' },
  createDisabled: { color: '#9CA3AF' },
  nameInput: {
    backgroundColor: 'white',
    marginHorizontal: 16,
    marginTop: 16,
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 10,
    fontSize: 16,
    color: '#111827',
  },
  searchInput: {
    backgroundColor: 'white',
    marginHorizontal: 16,
    marginBottom: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 10,
    color: '#111827',
  },
  count: {
    marginHorizontal: 16,
    marginVertical: 10,
    fontSize: 13,
    color: '#6b7280',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  avatar: { width: 42, height: 42, borderRadius: 21 },
  name: { flex: 1, marginLeft: 12, fontSize: 16, fontWeight: '500' },
  empty: { textAlign: 'center', marginTop: 40, color: '#6b7280' },
});

export default CreateGroupScreen;
